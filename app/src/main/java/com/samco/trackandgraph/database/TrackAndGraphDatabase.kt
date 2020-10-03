/* 
* This file is part of Life Hacking
* 
* Life Hacking is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Life Hacking is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Life Hacking.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.database

import android.content.Context
import androidx.room.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.dto.*
import com.samco.trackandgraph.database.entity.*
import org.threeten.bp.Duration
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat

const val MAX_FEATURE_NAME_LENGTH = 40
const val MAX_LABEL_LENGTH = 30
const val MAX_DISCRETE_VALUES_PER_FEATURE = 10
const val MAX_GRAPH_STAT_NAME_LENGTH = 100
const val MAX_GROUP_NAME_LENGTH = 40

val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
val doubleFormatter = DecimalFormat("#.##################")

//this is a number coprime to the number of colours used to select them in a pseudo random order for greater contrast
const val dataVisColorGenerator = 7
val dataVisColorList = listOf(
    R.color.visColor1,
    R.color.visColor2,
    R.color.visColor3,
    R.color.visColor4,
    R.color.visColor5,
    R.color.visColor6,
    R.color.visColor7,
    R.color.visColor8,
    R.color.visColor9,
    R.color.visColor10,
    R.color.visColor11,
    R.color.visColor12
)

const val splitChars1 = "||"
const val splitChars2 = "!!"

@Database(
    entities = [TrackGroup::class, Feature::class, DataPoint::class, GraphStatGroup::class,
        GraphOrStat::class, LineGraph::class, AverageTimeBetweenStat::class, PieChart::class,
        TimeSinceLastStat::class, Reminder::class, GlobalNote::class, LineGraphFeature::class,
        TimeHistogram::class],
    version = 42
)
@TypeConverters(Converters::class)
abstract class TrackAndGraphDatabase : RoomDatabase() {
    abstract val trackAndGraphDatabaseDao: TrackAndGraphDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: TrackAndGraphDatabase? = null
        fun getInstance(context: Context): TrackAndGraphDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room
                        .databaseBuilder(
                            context.applicationContext,
                            TrackAndGraphDatabase::class.java,
                            "trackandgraph_database"
                        )
                        .addMigrations(*allMigrations)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun stringToListOfDiscreteValues(value: String): List<DiscreteValue> {
        return if (value.isEmpty()) listOf()
        else value.split(splitChars1).map { s -> DiscreteValue.fromString(s) }.toList()
    }

    @TypeConverter
    fun listOfDiscreteValuesToString(values: List<DiscreteValue>): String =
        values.joinToString(splitChars1) { v -> v.toString() }

    @TypeConverter
    fun intToFeatureType(i: Int): FeatureType = FeatureType.values()[i]

    @TypeConverter
    fun featureTypeToInt(featureType: FeatureType): Int = featureType.ordinal

    @TypeConverter
    fun intToGraphStatType(i: Int): GraphStatType = GraphStatType.values()[i]

    @TypeConverter
    fun graphStatTypeToInt(graphStat: GraphStatType): Int = graphStat.ordinal

    @TypeConverter
    fun stringToOffsetDateTime(value: String?): OffsetDateTime? =
        value?.let { odtFromString(value) }

    @TypeConverter
    fun offsetDateTimeToString(value: OffsetDateTime?): String =
        value?.let { stringFromOdt(it) } ?: ""

    @TypeConverter
    fun durationToString(value: Duration?): String = value?.let { value.toString() } ?: ""

    @TypeConverter
    fun stringToDuration(value: String): Duration? =
        if (value.isEmpty()) null else Duration.parse(value)

    @TypeConverter
    fun intToGroupItemType(i: Int) = GroupItemType.values()[i]

    @TypeConverter
    fun localTimeToString(value: LocalTime) = value.toString()

    @TypeConverter
    fun localTimeFromString(value: String) = LocalTime.parse(value)

    @TypeConverter
    fun checkedDaysToString(value: CheckedDays) = value.toList().joinToString(splitChars1)

    @TypeConverter
    fun checkedDaysFromString(value: String): CheckedDays {
        return CheckedDays.fromList(
            value.split(splitChars1)
                .map { s -> s.toBoolean() }
        )
    }

    @TypeConverter
    fun yRangeTypeToInt(yRangeType: YRangeType) = yRangeType.ordinal

    @TypeConverter
    fun intToYRangeType(index: Int) = YRangeType.values()[index]

    @TypeConverter
    fun intToNoteType(index: Int) = NoteType.values()[index]

    @TypeConverter
    fun listOfIntsToString(ints: List<Int>) = ints.joinToString(splitChars1) { i ->
        i.toString()
    }

    @TypeConverter
    fun stringToListOfInts(intsString: String) = intsString.split(splitChars1).mapNotNull {
        it.toIntOrNull()
    }

    @TypeConverter
    fun averagingModeToInt(averaginMode: LineGraphAveraginModes) = averaginMode.ordinal

    @TypeConverter
    fun intToAveragingMode(index: Int) = LineGraphAveraginModes.values()[index]

    @TypeConverter
    fun lineGraphPlottingModeToInt(lineGraphPlottingMode: LineGraphPlottingModes) = lineGraphPlottingMode.ordinal

    @TypeConverter
    fun intToLineGraphPlottingMode(index: Int) = LineGraphPlottingModes.values()[index]

    @TypeConverter
    fun lineGraphPointStyleToInt(lineGraphPointStyle: LineGraphPointStyle) = lineGraphPointStyle.ordinal

    @TypeConverter
    fun intToLineGraphPointStyle(index: Int) = LineGraphPointStyle.values()[index]

    @TypeConverter
    fun durationPlottingModeToInt(durationPlottingMode: DurationPlottingMode) = durationPlottingMode.ordinal

    @TypeConverter
    fun intToDurationPlottingMode(index: Int) = DurationPlottingMode.values()[index]

    @TypeConverter
    fun timeHistogramWindowToInt(window: TimeHistogramWindow) = window.ordinal

    @TypeConverter
    fun intToTimeHistogramWindow(index: Int) = TimeHistogramWindow.values()[index]
}

fun odtFromString(value: String): OffsetDateTime? =
    if (value.isEmpty()) null
    else databaseFormatter.parse(value, OffsetDateTime::from)

fun stringFromOdt(value: OffsetDateTime): String = databaseFormatter.format(value)

