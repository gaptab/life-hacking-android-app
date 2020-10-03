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

import android.content.Context
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.ui.GraphLegendItemView

internal fun inflateGraphLegendItem(
    binding: GraphStatViewBinding, context: Context,
    colorIndex: Int, label: String
) {
    val colorId = dataVisColorList[colorIndex]
    binding.legendFlexboxLayout.addView(
        GraphLegendItemView(
            context,
            colorId,
            label
        )
    )
}
