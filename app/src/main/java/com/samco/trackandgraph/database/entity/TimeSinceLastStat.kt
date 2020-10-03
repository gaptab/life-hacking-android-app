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
import androidx.room.PrimaryKey
import org.threeten.bp.OffsetDateTime

@Entity(tableName = "time_since_last_stat_table2",
    foreignKeys = [
        ForeignKey(
            entity = GraphOrStat::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("graph_stat_id"),
            onDelete = ForeignKey.CASCADE),
        ForeignKey(
            entity = Feature::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("feature_id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimeSinceLastStat(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @ColumnInfo(name = "feature_id", index = true)
    val featureId: Long,

    @ColumnInfo(name = "from_value")
    val fromValue: String,

    @ColumnInfo(name = "to_value")
    val toValue: String,

    @ColumnInfo(name = "discrete_values")
    val discreteValues: List<Int>
)
