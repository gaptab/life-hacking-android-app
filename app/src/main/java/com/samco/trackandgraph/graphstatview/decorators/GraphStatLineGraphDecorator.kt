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
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import com.androidplot.ui.VerticalPosition
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.database.dto.*
import com.samco.trackandgraph.database.entity.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.graphstatview.*
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.util.formatDayMonth
import com.samco.trackandgraph.util.formatMonthYear
import com.samco.trackandgraph.util.formatTimeDuration
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

class GraphStatLineGraphDecorator(listMode: Boolean) :
    GraphStatViewDecorator<ILineGraphViewData>(listMode) {
    private val lineGraphHourMinuteSecondFromat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var graphStatView: IDecoratableGraphStatView? = null
    private var data: ILineGraphViewData? = null

    override suspend fun decorate(view: IDecoratableGraphStatView, data: ILineGraphViewData) {
        this.data = data
        this.graphStatView = view
        binding = view.getBinding()
        context = view.getContext()

        yield()
        withContext(Dispatchers.Default) { initFromLineGraphBody() }
    }

    override fun setTimeMarker(time: OffsetDateTime) {
        binding!!.xyPlot.removeMarkers()
        val markerPaint = getMarkerPaint()
        val millis = Duration.between(data!!.endTime, time).toMillis()
        binding!!.xyPlot.addMarker(
            XValueMarker(
                millis,
                null,
                VerticalPosition(0f, VerticalPositioning.ABSOLUTE_FROM_TOP),
                markerPaint,
                null
            )
        )
        binding!!.xyPlot.redraw()
    }

    private suspend fun initFromLineGraphBody() {
        yield()
        withContext(Dispatchers.Main) {
            binding!!.xyPlot.visibility = View.INVISIBLE
            binding!!.progressBar.visibility = View.VISIBLE
        }
        if (data!!.hasPlottableData) {
            drawLineGraphFeatures()
            setUpLineGraphXAxis()
            setUpLineGraphYAxis()
            setLineGraphBounds()
            binding!!.xyPlot.redraw()
            withContext(Dispatchers.Main) {
                binding!!.xyPlot.visibility = View.VISIBLE
                binding!!.progressBar.visibility = View.GONE
            }
        } else {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_graph)
        }
    }

    private suspend fun setLineGraphBounds() {
        val bounds = data!!.bounds
        if (data!!.yRangeType == YRangeType.FIXED) {
            binding!!.xyPlot.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
        }
        binding!!.xyPlot.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        binding!!.xyPlot.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        setLineGraphPaddingFromBounds(bounds)
    }

    private suspend fun setLineGraphPaddingFromBounds(bounds: RectRegion) {
        val minY = bounds.minY.toDouble()
        val maxY = bounds.maxY.toDouble()
        val maxBound = max(abs(minY), abs(maxY))
        val numDigits = log10(maxBound).toFloat() + 3
        yield()
        binding!!.xyPlot.graph.paddingLeft =
            (numDigits - 1) * (context!!.resources.displayMetrics.scaledDensity) * 3.5f
        binding!!.xyPlot.graph.refreshLayout()
    }

    private fun setUpLineGraphYAxis() {
        binding!!.xyPlot.setRangeStep(StepMode.SUBDIVIDE, 11.0)
        if (data!!.durationBasedRange) {
            binding!!.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.LEFT).format =
                object : Format() {
                    override fun format(
                        obj: Any,
                        toAppendTo: StringBuffer,
                        pos: FieldPosition
                    ): StringBuffer {
                        val sec = (obj as Number).toLong()
                        return toAppendTo.append(formatTimeDuration(sec))
                    }

                    override fun parseObject(source: String, pos: ParsePosition) = null
                }
        }
    }

    private fun setUpLineGraphXAxis() {
        binding!!.xyPlot.domainTitle.text = ""
        binding!!.xyPlot.setDomainStep(StepMode.SUBDIVIDE, 11.0)
        binding!!.xyPlot.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val millis = (obj as Number).toLong()
                    val duration = Duration.ofMillis(millis)
                    val timeStamp = data!!.endTime.plus(duration)
                    val formattedTimestamp = getDateTimeFormattedForDuration(
                        binding!!.xyPlot.bounds.minX,
                        binding!!.xyPlot.bounds.maxX,
                        timeStamp
                    )
                    return toAppendTo.append(formattedTimestamp)
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }

    private fun getDateTimeFormattedForDuration(
        minX: Number?,
        maxX: Number?,
        timestamp: OffsetDateTime
    ): String {
        if (minX == null || maxX == null) return formatDayMonth(context!!, timestamp)
        val duration = Duration.ofMillis(abs(maxX.toLong() - minX.toLong()))
        return when {
            duration.toMinutes() < 5L -> lineGraphHourMinuteSecondFromat.format(timestamp)
            duration.toDays() >= 304 -> formatMonthYear(context!!, timestamp)
            duration.toDays() >= 1 -> formatDayMonth(context!!, timestamp)
            else -> lineGraphHoursDateFormat.format(timestamp)
        }
    }

    private suspend fun drawLineGraphFeatures() {
        for (kvp in data!!.plottableData) {
            withContext(Dispatchers.Main) {
                inflateGraphLegendItem(binding!!, context!!, kvp.key.colorIndex, kvp.key.name)
            }
            kvp.value?.let { addSeries(it, kvp.key) }
        }
    }

    private suspend fun addSeries(series: FastXYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat =
            if (listMode && lineGraphFeature.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS)
                getFastLineAndPointFormatter(lineGraphFeature)
            else getLineAndPointFormatter(lineGraphFeature)
        yield()
        binding!!.xyPlot.addSeries(series, seriesFormat)
    }

    private fun getLineAndPointFormatter(lineGraphFeature: LineGraphFeature): LineAndPointFormatter {
        val formatter = LineAndPointFormatter()
        formatter.linePaint.apply {
            color = getLinePaintColor(lineGraphFeature)
            strokeWidth = getLinePaintWidth()
        }
        getVertexPaintColor(lineGraphFeature)?.let {
            formatter.vertexPaint.color = it
            formatter.vertexPaint.strokeWidth = getVertexPaintWidth()
        } ?: run {
            formatter.vertexPaint = null
        }
        getPointLabelFormatter(lineGraphFeature)?.let {
            formatter.pointLabelFormatter = it
            formatter.setPointLabeler { series, index ->
                DecimalFormat("#.#").format(series.getY(index))
            }
        } ?: run {
            formatter.pointLabelFormatter = null
        }
        formatter.fillPaint = null
        return formatter
    }

    private fun getFastLineAndPointFormatter(lineGraphFeature: LineGraphFeature): LineAndPointFormatter {
        val formatter = FastLineAndPointRenderer.Formatter(
            getLinePaintColor(lineGraphFeature),
            getVertexPaintColor(lineGraphFeature),
            getPointLabelFormatter(lineGraphFeature)
        )
        formatter.linePaint?.apply { isAntiAlias = false }
        formatter.linePaint?.apply { strokeWidth = getLinePaintWidth() }
        formatter.vertexPaint?.apply { strokeWidth = getVertexPaintWidth() }
        return formatter
    }

    private fun getLinePaintWidth() =
        context!!.resources.getDimension(R.dimen.line_graph_line_thickness)

    private fun getVertexPaintWidth() =
        context!!.resources.getDimension(R.dimen.line_graph_vertex_thickness)

    private fun getLinePaintColor(lineGraphFeature: LineGraphFeature): Int {
        return getPaintColor(lineGraphFeature)
    }

    private fun getVertexPaintColor(lineGraphFeature: LineGraphFeature): Int? {
        return if (lineGraphFeature.pointStyle == LineGraphPointStyle.NONE) null
        else getPaintColor(lineGraphFeature)
    }

    private fun getPointLabelFormatter(lineGraphFeature: LineGraphFeature): PointLabelFormatter? {
        if (lineGraphFeature.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS) return null
        val color = context!!.getColorFromAttr(android.R.attr.textColorPrimary)
        val pointLabelFormatter = PointLabelFormatter(
            color,
            context!!.resources.getDimension(R.dimen.line_graph_point_label_h_offset),
            context!!.resources.getDimension(R.dimen.line_graph_point_label_v_offset)
        )
        pointLabelFormatter.textPaint.textAlign = Paint.Align.RIGHT
        return pointLabelFormatter
    }

    private fun getPaintColor(lineGraphFeature: LineGraphFeature) =
        ContextCompat.getColor(context!!, dataVisColorList[lineGraphFeature.colorIndex])

    private fun getMarkerPaint(): Paint {
        val color = context!!.getColorFromAttr(R.attr.errorTextColor)
        val paint = Paint()
        paint.color = color
        paint.strokeWidth = 2f
        return paint
    }
}
