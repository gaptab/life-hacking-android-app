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
package com.samco.trackandgraph.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import com.samco.trackandgraph.database.doubleFormatter
import com.samco.trackandgraph.util.formatTimeDuration
import org.threeten.bp.OffsetDateTime

@Entity(
    tableName = "data_points_table",
    primaryKeys = ["timestamp", "feature_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DataPoint(
    @ColumnInfo(name = "timestamp")
    val timestamp: OffsetDateTime = OffsetDateTime.now(),

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "value")
    val value: Double,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "note")
    val note: String
) {
    companion object {
        fun getDisplayValue(dataPoint: DataPoint, featureType: FeatureType): String {
            return when (featureType) {
                FeatureType.DISCRETE -> doubleFormatter.format(dataPoint.value) + " : ${dataPoint.label}"
                FeatureType.CONTINUOUS -> doubleFormatter.format(dataPoint.value)
                FeatureType.DURATION -> formatTimeDuration(dataPoint.value.toLong())
            }
        }
    }
}