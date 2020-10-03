/*
 *  This file is part of Life Hacking
 *
 *  Life Hacking is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Life Hacking is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Life Hacking.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.statistics

import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.FastXYSeries
import com.androidplot.xy.RectRegion
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.TemporalAmount
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.math.abs
import kotlin.math.min

class DataSample(val dataPoints: List<DataPoint>)

/**
 * This function will call the dataSource to get a sample of the data points with the given featureId.
 * The end date of this sample is calculated as:
 *  - If an endDate is given then it is used
 *  - If an endDate is not given then the last data point tracked or the current date/time is used (which ever is later)
 * The start date of this sample is calculated as:
 *  - The beginning of time if no sampleDuration is provided
 *  - If a sampleDuration is provided then it is the end date minus the sample duration. However if there is
 *      a plotTotalTime or averagingDuration provided as well then which ever of the two is larger will be added
 *      to the sampleDuration before it is subtracted from the end date such that all relevant information
 *      is contained in the sample.
 *
 * Note: No actual averaging or totalling is performed by this function, it just collects all relevant data.
 */
internal suspend fun sampleData(
    dataSource: TrackAndGraphDatabaseDao, featureId: Long, sampleDuration: Duration?,
    endDate: OffsetDateTime?, averagingDuration: Duration?, plotTotalTime: TemporalAmount?
): DataSample {
    return withContext(Dispatchers.IO) {
        if (sampleDuration == null && endDate == null) DataSample(
            dataSource.getDataPointsForFeatureAscSync(featureId)
        )
        else {
            val latest = endDate ?: getLastTrackedTimeOrNow(
                dataSource,
                featureId
            )
            val plottingDuration =
                plotTotalTime?.let { Duration.between(latest, latest.plus(plotTotalTime)) }
            val minSampleDate = sampleDuration?.let {
                val possibleLongestDurations = listOf(
                    sampleDuration,
                    averagingDuration?.plus(sampleDuration),
                    plottingDuration?.plus(sampleDuration)
                )
                latest.minus(possibleLongestDurations.maxBy { d -> d ?: Duration.ZERO })
            } ?: OffsetDateTime.MIN
            val dataPoints =
                dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
            DataSample(
                dataPoints
            )
        }
    }
}

private fun getLastTrackedTimeOrNow(
    dataSource: TrackAndGraphDatabaseDao,
    featureId: Long
): OffsetDateTime {
    val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
    val now = OffsetDateTime.now()
    val latest = lastDataPointList.firstOrNull()?.timestamp?.plusSeconds(1)
    return listOfNotNull(now, latest).max()!!
}

/**
 * Add up all data points per plotTotalTime. For example if the plot total time is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample containing 1 point with the value 11.
 *
 * The currently supported plotTotalTime values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * If sampleDuration is provided then totals will be generated at least as far back as now minus the
 * sampleDuration. However if there is more data before this period then it that data will also be
 * totalled. For clipping see clipDataSample.
 *
 * If end time is provided then totals will be generated at least up to this time. However if there
 * is more data after the end time in the input sample then that data will also be totalled. For
 * clipping see clipDataSample.
 *
 * All notes and labels will be lost in the output data.
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */
internal suspend fun calculateDurationAccumulatedValues(
    sampleData: DataSample,
    featureId: Long,
    sampleDuration: Duration?,
    endTime: OffsetDateTime?,
    plotTotalTime: TemporalAmount
): DataSample {
    val newData = mutableListOf<DataPoint>()
    val latest = getEndTimeNowOrLatest(sampleData, endTime)
    val firstDataPointTime = getStartTimeOrFirst(sampleData, latest, endTime, sampleDuration)
    var currentTimeStamp = findBeginningOfTemporal(firstDataPointTime, plotTotalTime).minusNanos(1)
    var index = 0
    while (currentTimeStamp.isBefore(latest)) {
        currentTimeStamp = currentTimeStamp.with { ld -> ld.plus(plotTotalTime) }
        val points = sampleData.dataPoints.drop(index)
            .takeWhile { dp -> dp.timestamp.isBefore(currentTimeStamp) }
        val total = points.sumByDouble { dp -> dp.value }
        index += points.size
        newData.add(DataPoint(currentTimeStamp, featureId, total, "", ""))
        yield()
    }
    return DataSample(newData)
}

private fun getStartTimeOrFirst(
    sampleData: DataSample,
    latest: OffsetDateTime,
    endTime: OffsetDateTime?,
    sampleDuration: Duration?
): OffsetDateTime {
    val firstDataPointTime = sampleData.dataPoints.firstOrNull()?.timestamp
    val beginningOfDuration = sampleDuration?.let { endTime?.minus(it) }
    val durationBeforeLatest = sampleDuration?.let { latest.minus(it) }
    return listOf(
        firstDataPointTime,
        beginningOfDuration,
        durationBeforeLatest,
        latest
    ).minBy { t -> t ?: OffsetDateTime.MAX }!!
}

private fun getEndTimeNowOrLatest(rawData: DataSample, endTime: OffsetDateTime?): OffsetDateTime {
    val now = OffsetDateTime.now()
    val last = rawData.dataPoints.lastOrNull()?.timestamp
    return when {
        //last == null && endTime == null -> now
        endTime == null -> listOf(last, now).maxBy { t -> t ?: OffsetDateTime.MIN }!!
        else -> listOf(last, endTime).maxBy { t -> t ?: OffsetDateTime.MIN }!!
    }
}

/**
 * Finds the first ending of temporalAmount before dateTime. For example if temporalAmount is a
 * week then it will find the very end of the sunday before dateTime.
 *
 * temporalAmount supports the use of duration instead of Period.
 * In this case the function will try to approximate the behaviour as closely as possible as though
 * temporalAmount was a period. For example if you pass in a duration of 7 days it will try to find
 * the last day of the week before the week containing dateTime. It is always the largest recognised
 * duration that is used when deciding what the start of the period should be. The recognised durations
 * are:
 *
 *  Duration.ofHours(1)
 *  Duration.ofHours(24)
 *  Duration.ofDays(7)
 *  Duration.ofDays(30)
 *  Duration.ofDays(365 / 4) or 3 months
 *  Duration.ofDays(365 / 2) or 6 months
 *  Duration.ofDays(365) or a year
 *
 */
internal fun findBeginningOfTemporal(
    dateTime: OffsetDateTime,
    temporalAmount: TemporalAmount
): OffsetDateTime {
    return when (temporalAmount) {
        is Duration -> findBeginningOfDuration(dateTime, temporalAmount)
        is Period -> findBeginningOfPeriod(dateTime, temporalAmount)
        else -> dateTime
    }
}

private fun findBeginningOfDuration(
    dateTime: OffsetDateTime,
    duration: Duration
): OffsetDateTime {
    val dt = dateTime
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    val nano = Duration.ofNanos(1)
    val day = Duration.ofDays(1)
    return when {
        duration.minus(nano.plus(Duration.ofMinutes(60))).isNegative -> dt
        duration.minus(nano.plus(Duration.ofHours(24))).isNegative -> {
            dt.withHour(0)
        }
        duration.minus(nano.plus(Duration.ofDays(7))).isNegative -> {
            dt.with(
                TemporalAdjusters.previousOrSame(
                    WeekFields.of(Locale.getDefault()).firstDayOfWeek
                )
            ).withHour(0)
        }
        duration.minus(nano.plus(Duration.ofDays(31))).isNegative -> {
            dt.withDayOfMonth(1).withHour(0)
        }
        duration.minus(day.plus(Duration.ofDays(365 / 4))).isNegative -> {
            val month = getQuaterForMonthValue(dt.monthValue)
            dt.withMonth(month).withDayOfMonth(1).withHour(0)
        }
        duration.minus(day.plus(Duration.ofDays(365 / 2))).isNegative -> {
            val month = getBiYearForMonthValue(dt.monthValue)
            dt.withMonth(month).withDayOfMonth(1).withHour(0)
        }
        else -> dt.withDayOfYear(1).withHour(0)
    }
}

/**
 * Given a number representing a month in the range 1 to 12, this function will return you the
 * integer value of the month starting the quater of the year containing that month.
 */
internal fun getQuaterForMonthValue(monthValue: Int) = (3 * ((monthValue - 1) / 3)) + 1

/**
 * Given a number representing a month in the range 1 to 12, this function will return you the
 * integer value of the month starting the bi year containing that month.
 */
internal fun getBiYearForMonthValue(monthValue: Int) = if (monthValue < 7) 1 else 7

private fun findBeginningOfPeriod(
    dateTime: OffsetDateTime,
    period: Period
): OffsetDateTime {
    val dt = dateTime.withHour(0)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)

    return when {
        isPeriodNegativeOrZero(period.minus(Period.ofDays(1))) -> dt
        isPeriodNegativeOrZero(period.minus(Period.ofWeeks(1))) -> {
            val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
            dt.with(TemporalAdjusters.previousOrSame(firstDay))
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(1))) -> {
            dt.withDayOfMonth(1)
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(3))) -> {
            val month = getQuaterForMonthValue(dateTime.monthValue)
            dt.withMonth(month).withDayOfMonth(1)
        }
        isPeriodNegativeOrZero(period.minus(Period.ofMonths(6))) -> {
            val month = getBiYearForMonthValue(dateTime.monthValue)
            dt.withMonth(month).withDayOfMonth(1)
        }
        else -> dt.withDayOfYear(1)
    }
}

private fun isPeriodNegativeOrZero(period: Period) = period.years < 0
        || (period.years == 0 && period.months < 0)
        || (period.years == 0 && period.months == 0 && period.days <= 0)

/**
 * Calculate the moving averages of all of the data points given over the moving average duration given.
 * A new DataSample will be returned with one data point for every data point in the input set whose
 * timestamp shall be the same but value will be equal to the average of it and all previous data points
 * within the movingAvDuration.
 *
 * The data points in the input sample are expected to be in date order with the oldest data points
 * earliest in the list
 */
internal suspend fun calculateMovingAverages(
    dataSample: DataSample,
    movingAvDuration: Duration
): DataSample {
    val movingAverages = mutableListOf<DataPoint>()
    var currentAccumulation = 0.0
    var currentCount = 0
    var addIndex = 0
    val dataPoints = dataSample.dataPoints.reversed()
    for (index in dataPoints.indices) {
        yield()
        val current = dataPoints[index]
        while (addIndex < dataPoints.size
            && Duration.between(
                dataPoints[addIndex].timestamp,
                current.timestamp
            ) <= movingAvDuration
        ) {
            currentAccumulation += dataPoints[addIndex++].value
            currentCount++
        }
        val averageValue = currentAccumulation / currentCount.toDouble()
        movingAverages.add(
            0,
            DataPoint(
                current.timestamp,
                current.featureId,
                averageValue,
                current.label,
                current.note
            )
        )
        currentAccumulation -= current.value
        currentCount--
    }

    return DataSample(movingAverages)
}

/**
 * Return all the data points in the sample that lie within the sampleDuration leading up to the endTime.
 * If the sampleDuration is null then all data points leading up to the end time will be returned.
 * If the endTime is null then all data points within the sampleDuration leading up to the last data point
 * will be returned. If both the sampleDuration and endTime are null then the whole list will be returned.
 */
internal fun clipDataSample(
    dataSample: DataSample,
    endTime: OffsetDateTime?,
    sampleDuration: Duration?
): DataSample {
    if (dataSample.dataPoints.isEmpty()) return dataSample

    var newDataPoints = dataSample.dataPoints
    if (endTime != null) {
        val lastIndex = newDataPoints.indexOfLast { dp -> dp.timestamp <= endTime }
        newDataPoints = newDataPoints.take(lastIndex + 1)
    }
    if (sampleDuration != null) {
        val endOfDuration = endTime ?: dataSample.dataPoints.last().timestamp
        val startTime = endOfDuration.minus(sampleDuration)
        val firstIndex = newDataPoints.indexOfFirst { dp -> dp.timestamp >= startTime }
        newDataPoints = if (firstIndex < 0) emptyList() else newDataPoints.drop(firstIndex)
    }
    return DataSample(newDataPoints)
}

internal fun getXYSeriesFromDataSample(
    dataSample: DataSample,
    endTime: OffsetDateTime,
    lineGraphFeature: LineGraphFeature
): FastXYSeries {
    val scale = lineGraphFeature.scale
    val offset = lineGraphFeature.offset
    val durationDivisor = when (lineGraphFeature.durationPlottingMode) {
        DurationPlottingMode.HOURS -> 3600.0
        DurationPlottingMode.MINUTES -> 60.0
        else -> 1.0
    }
    val yValues = dataSample.dataPoints.map { dp ->
        (dp.value * scale / durationDivisor) + offset
    }
    val xValues =
        dataSample.dataPoints.map { dp -> Duration.between(endTime, dp.timestamp).toMillis() }

    var yRegion = SeriesUtils.minMax(yValues)
    if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
        yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)
    val xRegion = SeriesUtils.minMax(xValues)
    val rectRegion = RectRegion(xRegion.min, xRegion.max, yRegion.min, yRegion.max)

    return object : FastXYSeries {
        override fun minMax() = rectRegion
        override fun getX(index: Int): Number = xValues[index]
        override fun getY(index: Int): Number = yValues[index]
        override fun getTitle() = lineGraphFeature.name
        override fun size() = dataSample.dataPoints.size
    }
}

/**
 * Get the end of the window with respect to the given endDate. For example if the window
 * size is a week and the endDate is on a Wednesday then the returned date/time will be
 * 00:00 on the following monday.
 *
 * If the endDate is null then the current date/time is used in its place
 */
internal fun getNextEndOfWindow(
    window: TimeHistogramWindow,
    endDate: OffsetDateTime?
): OffsetDateTime {
    val end = endDate ?: OffsetDateTime.now()
    return findBeginningOfTemporal(
        end.plus(window.period),
        window.period
    )
}

/**
 * This function essentially loops over the data sample and puts every input data point into a bin
 * depending on where its timestamp falls within the given input window. For example if the window
 * represents a week then each data point may be put into one of 7 bins depending on which day of the
 * week it was tracked.
 *
 * A map is generated with a data structure similar to a Matrix whereby each value in the map is a list of the
 * same length. The keys in the map are the integer values of the discrete values of the {@param feature}
 * or just {0} if the feature is not Discrete. The length of the lists is the number of bins of the
 * given {@param window}. The lists represent the sum of all values in each bin of the histogram
 * normalised such that the sum of all values in all lists is 1.
 *
 * If {@param sumByCount} is false then the value of each data point is added to the total value of
 * the histogram bin it belongs in before normalisation. If sumByCount is true then the value of each
 * histogram bin before normalisation is the number of data points that fall in that bin.
 *
 * {@param sample} - The data points to generate a histogram for
 * {@param window} - The TimeHistogramWindow specifying the domain and number of bins of the histogram
 * {@param feature} - The Feature for which the histogram is being generated
 * {@param sumByCount} - Whether this histogram represents the number of data points tracked or
 * the sum of their values
 */
internal fun getHistogramBinsForSample(
    sample: DataSample,
    window: TimeHistogramWindow,
    feature: Feature,
    sumByCount: Boolean
): Map<Int, List<Double>>? {
    if (sample.dataPoints.isEmpty()) return null
    val endTime = getNextEndOfWindow(window, sample.dataPoints.maxBy { it.timestamp }!!.timestamp)
    val isDiscrete = feature.featureType == FeatureType.DISCRETE
    val keys =
        if (isDiscrete) feature.discreteValues.map { it.index }.toSet()
        else listOf(0).toSet()

    return when {
        isDiscrete && sumByCount -> getHistogramBinsForSample(
            sample.dataPoints, window, keys, endTime, ::addOneDiscreteValueToBin
        )
        isDiscrete -> getHistogramBinsForSample(
            sample.dataPoints, window, keys, endTime, ::addDiscreteValueToBin
        )
        sumByCount -> getHistogramBinsForSample(
            sample.dataPoints, window, keys, endTime, ::addOneToBin
        )
        else -> getHistogramBinsForSample(
            sample.dataPoints, window, keys, endTime, ::addValueToBin
        )
    }
}

private fun getHistogramBinsForSample(
    sample: List<DataPoint>,
    window: TimeHistogramWindow,
    keys: Set<Int>,
    endTime: OffsetDateTime,
    addFunction: (DataPoint, Map<Int, MutableList<Double>>, Int) -> Unit
): Map<Int, List<Double>> {
    val binTotalMaps = calculateBinTotals(sample, window, keys, endTime, addFunction)
    val total = binTotalMaps.map { it.value.sum() }.sum()
    return binTotalMaps.map { kvp -> kvp.key to kvp.value.map { it / total } }.toMap()
}

/**
 * Create a map structure and place every data point in it using the provided addFunction
 */
private fun calculateBinTotals(
    sample: List<DataPoint>,
    window: TimeHistogramWindow,
    keys: Set<Int>,
    endTime: OffsetDateTime,
    addFunction: (DataPoint, Map<Int, MutableList<Double>>, Int) -> Unit
): Map<Int, List<Double>> {
    val binTotalMap = keys.map { it to MutableList(window.numBins) { 0.0 } }.toMap()
    var currEnd = endTime
    var currStart = currEnd - window.period
    var binned = 0
    val reversed = sample.asReversed()
    var nextPoint = reversed[0]
    while (binned < sample.size) {
        val periodDuration = Duration
            .between(currStart, currEnd)
            .seconds
            .toDouble()
        while (nextPoint.timestamp > currStart) {
            val distance = Duration.between(currStart, nextPoint.timestamp).seconds.toDouble()
            var binIndex = (window.numBins * (distance / periodDuration)).toInt()
            if (binIndex == window.numBins) binIndex--
            addFunction(nextPoint, binTotalMap, binIndex)
            if (++binned == sample.size) break
            nextPoint = reversed[binned]
        }
        currEnd -= window.period
        currStart -= window.period
    }
    return binTotalMap
}

/**
 * Add the value of the given data point to the bin at the given binIndex
 */
private fun addValueToBin(dataPoint: DataPoint, bin: Map<Int, MutableList<Double>>, binIndex: Int) {
    bin[0]?.set(binIndex, (bin[0]?.get(binIndex) ?: 0.0) + dataPoint.value)
}

/**
 * Add one to the bin at the given binIndex
 */
private fun addOneToBin(dataPoint: DataPoint, bin: Map<Int, MutableList<Double>>, binIndex: Int) {
    bin[0]?.set(binIndex, (bin[0]?.get(binIndex) ?: 0.0) + 1.0)
}

/**
 * Add the value of the given data point to the bin at the given binIndex within the histogram
 * specific to its discrete value.
 */
private fun addDiscreteValueToBin(
    dataPoint: DataPoint,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    val i = dataPoint.value.toInt()
    bin[i]?.set(binIndex, (bin[i]?.get(binIndex) ?: 0.0) + dataPoint.value)
}

/**
 * Add one to the bin at the given binIndex within the histogram specific to its discrete value.
 */
private fun addOneDiscreteValueToBin(
    dataPoint: DataPoint,
    bin: Map<Int, MutableList<Double>>,
    binIndex: Int
) {
    val i = dataPoint.value.toInt()
    bin[i]?.set(binIndex, (bin[i]?.get(binIndex) ?: 0.0) + 1.0)
}

/**
 * Given an input list of lists where each list represents the discrete value of a feature and each
 * sub-list has the same size and represents the values of each histogram bin for that discrete value.
 * Calculate the largest bin by summing the values of each discrete value for each bin and returning
 * the largest of those sums.
 */
internal fun getLargestBin(bins: List<List<Double>>?): Double? {
    return bins
        ?.getOrElse(0) { null }
        ?.size
        ?.downTo(1)
        ?.map { index -> bins.sumByDouble { it[index - 1] } }
        ?.max()
}

