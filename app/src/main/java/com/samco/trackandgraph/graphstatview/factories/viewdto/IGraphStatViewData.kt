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

import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.graphstatview.GraphStatInitException

interface IGraphStatViewData {
    enum class State {
        LOADING,
        READY,
        ERROR
    }
    val state: State
    val graphOrStat: GraphOrStat
    val error: GraphStatInitException?
        get() = null

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object: IGraphStatViewData {
            override val state: State
                get() = State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }
}