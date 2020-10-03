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

package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.IAverageTimeBetweenViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

class AverageTimeBetweenDataFactory :
    ViewDataFactory<AverageTimeBetweenStat, IAverageTimeBetweenViewData>() {
    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val timeBetweenStat = dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id)
            ?: return notEnoughData(graphOrStat)
        return createViewData(dataSource, graphOrStat, timeBetweenStat, onDataSampled)
    }

    override suspend fun createViewData(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat,
        config: AverageTimeBetweenStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IAverageTimeBetweenViewData {
        val feature = dataSource.getFeatureById(config.featureId)
        val dataPoints = getRelevantDataPoints(dataSource, config, feature)
        if (dataPoints.size < 2) return notEnoughData(graphOrStat)
        val now = OffsetDateTime.now()
        val last = dataPoints.last().timestamp
        val latest = listOf(now, last).max() ?: now
        val start = config.duration?.let { latest.minus(it) } ?: dataPoints.first().timestamp
        val totalMillis = Duration.between(start, latest).toMillis().toDouble()
        val durationMillis = when (feature.featureType) {
            //TODO We want to exclude time tracked but the first data point may overlap the beginning
            // and in fact will necessarily always be entirely before the beginning of the period
            // if the duration passed in is null. This does mean the accuracy may be poor under certain
            // conditions. This could be fixed by clipping the first data point duration.
            FeatureType.DURATION -> dataPoints.drop(1).sumByDouble { it.value }
            else -> 0.0
        }
        val totalGapMillis =
            if (durationMillis > totalMillis) totalMillis
            else totalMillis - durationMillis
        val averageMillis = totalGapMillis / (dataPoints.size - 1).toDouble()
        onDataSampled(dataPoints)
        return object : IAverageTimeBetweenViewData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
            override val averageMillis: Double
                get() = averageMillis
            override val hasEnoughData: Boolean
                get() = true
        }
    }

    private fun notEnoughData(graphOrStat: GraphOrStat) = object : IAverageTimeBetweenViewData {
        override val state: IGraphStatViewData.State
            get() = IGraphStatViewData.State.READY
        override val graphOrStat: GraphOrStat
            get() = graphOrStat
    }

    private fun getRelevantDataPoints(
        dataSource: TrackAndGraphDatabaseDao,
        timeBetweenStat: AverageTimeBetweenStat,
        feature: Feature
    ): List<DataPoint> {
        val endDate = timeBetweenStat.endDate ?: OffsetDateTime.now()
        val startDate =
            timeBetweenStat.duration?.let { endDate.minus(it) } ?: OffsetDateTime.MIN
        return when (feature.featureType) {
            FeatureType.CONTINUOUS, FeatureType.DURATION -> {
                dataSource.getDataPointsBetweenInTimeRange(
                    feature.id,
                    timeBetweenStat.fromValue,
                    timeBetweenStat.toValue,
                    startDate,
                    endDate
                )
            }
            FeatureType.DISCRETE -> {
                dataSource.getDataPointsWithValueInTimeRange(
                    feature.id,
                    timeBetweenStat.discreteValues,
                    startDate,
                    endDate
                )
            }
        }
    }
}