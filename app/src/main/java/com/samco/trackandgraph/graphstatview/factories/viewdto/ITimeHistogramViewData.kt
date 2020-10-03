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

package com.samco.trackandgraph.graphstatview.factories.viewdto

import com.samco.trackandgraph.database.entity.DiscreteValue
import com.samco.trackandgraph.database.entity.TimeHistogramWindow

interface ITimeHistogramViewData : IGraphStatViewData {
    val window: TimeHistogramWindow?
        get() = null
    val discreteValues: List<DiscreteValue>?
        get() = null
    val barValues: Map<Int, List<Double>>?
        get() = null
    val maxDisplayHeight: Double?
        get() = 0.0
}