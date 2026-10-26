package com.example.finalafter.crnoui.core.perfhud.panel.graph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.example.finalafter.crnoui.core.designsystem.theme.AppTheme
import com.example.finalafter.crnoui.core.perfhud.R
import com.example.finalafter.crnoui.core.perfhud.frame.FrameHistogram
import com.example.finalafter.crnoui.core.perfhud.frame.FrameLog
import java.util.Locale
import kotlin.math.min

/**
 * Every frame recorded, counted by its main-thread time: one bar per millisecond below
 * [LastMillis], and one for [LastMillis] and slower, with one refresh interval as a line, the time
 * the main thread has for each frame. A healthy app is a tall bar left of the line and almost
 * nothing right of it. Main-thread time rather than the full duration, which the render pipeline
 * lets run past one interval in frames that are on time.
 *
 * [budgetMillis] is the interval at the display's rate while drawing (`FrameSummary.refreshHz`):
 * a frame that an adaptive display drew at a lower rate had more time than the line shows.
 */
@Composable
internal fun HistogramGraph(
    frames: FrameLog,
    budgetMillis: () -> Float,
    version: () -> Int,
    modifier: Modifier = Modifier,
) {
    val histogram = remember { FrameHistogram() }
    val bins = remember { IntArray(Bins) }
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.caption1
    val axisFormat = stringResource(R.string.perfhud_millis_axis)
    val colors = AppTheme.colors
    val inside = colors.success.content
    val outside = colors.warning.content
    val guide = colors.danger.content
    val axis = colors.separator
    val label = colors.contentTertiary

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphDefaults.Height)
            .drawWithCache {
                val ticks = Ticks.map { millis ->
                    measurer.measure(String.format(Locale.ROOT, axisFormat, millis), labelStyle)
                }
                val labelHeight = ticks.maxOf { it.size.height }.toFloat()
                val stroke = GraphDefaults.Stroke.toPx()
                onDrawBehind {
                    version()
                    frames.copyMainThreadHistogram(histogram)
                    binByMillisecond(histogram, bins)
                    val plotHeight = size.height - labelHeight
                    val slot = size.width / Bins
                    val highest = bins.max().coerceAtLeast(1)
                    val budget = budgetMillis()
                    for (bin in 0 until Bins) {
                        val height = bins[bin].toFloat() / highest * plotHeight
                        drawRect(
                            color = if (bin + 1 <= budget) inside else outside,
                            topLeft = Offset(bin * slot, plotHeight - height),
                            size = Size(slot * BarShare, height),
                        )
                    }
                    if (budget > 0f) {
                        val x = budget * slot
                        drawLine(guide, Offset(x, 0f), Offset(x, plotHeight), stroke)
                    }
                    drawLine(axis, Offset(0f, plotHeight), Offset(size.width, plotHeight), stroke)
                    for (index in Ticks.indices) {
                        // The last tick is near the right edge, and its label stays inside the graph.
                        val x = min(Ticks[index] * slot, size.width - ticks[index].size.width)
                        drawText(ticks[index], color = label, topLeft = Offset(x, plotHeight))
                    }
                }
            },
    )
}

/**
 * Folds the histogram's buckets into whole milliseconds, the last bin open-ended. The buckets are
 * a quarter of a millisecond wide below [LastMillis] and start at or past it from there on, so none
 * straddles a bin's edge.
 */
private fun binByMillisecond(histogram: FrameHistogram, into: IntArray) {
    into.fill(0)
    for (bucket in 0 until FrameHistogram.BUCKETS) {
        val count = histogram.count(bucket)
        if (count == 0) continue
        val bin = FrameHistogram.lowerEdge(bucket).toInt().coerceAtMost(Bins - 1)
        into[bin] += count
    }
}

private const val LastMillis = 32
private const val Bins = LastMillis + 1
private val Ticks = intArrayOf(0, 8, 16, 24, 32)
private const val BarShare = 0.8f
