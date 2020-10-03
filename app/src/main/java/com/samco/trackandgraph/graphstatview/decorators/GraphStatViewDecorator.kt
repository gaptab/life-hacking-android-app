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

package com.samco.trackandgraph.graphstatview.decorators

import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import org.threeten.bp.OffsetDateTime

interface IGraphStatViewDecorator {
    fun setTimeMarker(time: OffsetDateTime)
}

abstract class GraphStatViewDecorator<T : IGraphStatViewData>(protected val listMode: Boolean) :
    IGraphStatViewDecorator {
    abstract suspend fun decorate(view: IDecoratableGraphStatView, data: T)
}