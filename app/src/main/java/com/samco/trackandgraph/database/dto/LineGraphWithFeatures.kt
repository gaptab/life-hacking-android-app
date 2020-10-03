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
package com.samco.trackandgraph.database.dto

import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.samco.trackandgraph.database.entity.LineGraph
import com.samco.trackandgraph.database.entity.LineGraphFeature
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

enum class YRangeType {
    DYNAMIC,
    FIXED
}

data class LineGraphWithFeatures(
    @ColumnInfo(name = "id", index = true)
    val id: Long,

    @ColumnInfo(name = "graph_stat_id", index = true)
    val graphStatId: Long,

    @Relation(parentColumn = "id", entityColumn = "line_graph_id", entity = LineGraphFeature::class)
    val features: List<LineGraphFeature>,

    @ColumnInfo(name = "duration")
    val duration: Duration?,

    @ColumnInfo(name = "y_range_type")
    val yRangeType: YRangeType,

    @ColumnInfo(name = "y_from")
    val yFrom: Double,

    @ColumnInfo(name = "y_to")
    val yTo: Double,

    @ColumnInfo(name = "end_date")
    val endDate: OffsetDateTime?
) {
    fun toLineGraph() = LineGraph(id, graphStatId, duration, yRangeType, yFrom, yTo, endDate)
}
