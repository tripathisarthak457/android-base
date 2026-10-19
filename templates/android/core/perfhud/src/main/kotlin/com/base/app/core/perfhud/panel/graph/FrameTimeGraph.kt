package com.base.app.core.perfhud.panel.graph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.perfhud.R
import com.base.app.core.perfhud.frame.FrameLog
import com.base.app.core.perfhud.frame.FrameSeries
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * The last frames as bars against the frame budget at 60, 90 and 120 Hz. Each bar is the frame's
 * full duration, vsync to GPU done, in a faded tone, with its main-thread time solid at the foot:
 * red for a frame that missed its deadline, amber for one that made it although its main thread
 * took longer than the display's budget, green otherwise. The full duration is not judged against
 * one budget, since the render pipeline gives a frame more time than that and on-time frames
 * routinely run past it.
 *
 * Drawn from [frames] in the draw phase each time [version] changes, into a series allocated once.
 */
@Composable
internal fun FrameTimeGraph(frames: FrameLog, version: () -> Int, modifier: Modifier = Modifier) {
    val series = remember { FrameSeries(Bars) }
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.caption
    val hzFormat = stringResource(R.string.perfhud_hz)
    val colors = AppTheme.colors
    val good = colors.success.content
    val over = colors.warning.content
    val missed = colors.danger.content
    val goodFaded = good.copy(alpha = FadedAlpha)
    val overFaded = over.copy(alpha = FadedAlpha)
    val missedFaded = missed.copy(alpha = FadedAlpha)
    val guide = colors.contentTertiary
    val axis = colors.divider

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphDefaults.Height)
            .drawWithCache {
                val labels = BudgetHz.map { hz ->
                    measurer.measure(String.format(Locale.ROOT, hzFormat, hz), labelStyle)
                }
                val labelGap = LabelGap.toPx()
                // The budget labels get a column of their own, since over the bars they would be unreadable.
                val gutter = labels.maxOf { it.size.width } + labelGap
                val dash = PathEffect.dashPathEffect(floatArrayOf(DashPx, DashPx))
                val stroke = GraphDefaults.Stroke.toPx()
                onDrawBehind {
                    version()
                    frames.copyRecent(series)
                    val barWidth = (size.width - gutter) / Bars
                    val gap = if (barWidth > MinGappedBarPx) 1f else 0f
                    val start = size.width - series.count * barWidth
                    for (i in 0 until series.count) {
                        val total = series.totalMillis[i]
                        val ui = min(series.uiMillis[i], total)
                        val missedDeadline = series.janky[i]
                        val uiOver = ui > series.intervalMillis[i]
                        val solid = when {
                            missedDeadline -> missed
                            uiOver -> over
                            else -> good
                        }
                        val faded = when {
                            missedDeadline -> missedFaded
                            uiOver -> overFaded
                            else -> goodFaded
                        }
                        val left = start + i * barWidth
                        val totalHeight = min(total / ScaleMillis, 1f) * size.height
                        val uiHeight = min(ui / ScaleMillis, 1f) * size.height
                        drawRect(
                            color = faded,
                            topLeft = Offset(left, size.height - totalHeight),
                            size = Size(barWidth - gap, totalHeight - uiHeight),
                        )
                        drawRect(
                            color = solid,
                            topLeft = Offset(left, size.height - uiHeight),
                            size = Size(barWidth - gap, uiHeight),
                        )
                    }
                    var labelBottom = Float.NEGATIVE_INFINITY
                    for (index in BudgetHz.indices) {
                        val y = size.height * (1f - MillisPerSecond / BudgetHz[index] / ScaleMillis)
                        drawLine(guide, Offset(gutter, y), Offset(size.width, y), stroke, pathEffect = dash)
                        val label = labels[index]
                        // The 90 and 120 Hz lines lie closer together than a label is tall, so a label
                        // that would overlap the one above it moves down just clear of it.
                        val top = max(y - label.size.height / 2f, labelBottom)
                        drawText(label, color = guide, topLeft = Offset(gutter - labelGap - label.size.width, top))
                        labelBottom = top + label.size.height
                    }
                    drawLine(axis, Offset(gutter, size.height), Offset(size.width, size.height), stroke)
                }
            },
    )
}

/** About two seconds of drawing at 120 Hz, each bar a little over a pixel wide on a phone. */
private const val Bars = 240

/** The graph's top: two frames at 60 Hz. A taller frame is drawn to the top. */
private const val ScaleMillis = 33.3f

private val BudgetHz = intArrayOf(60, 90, 120)
private const val MillisPerSecond = 1000f
private const val DashPx = 8f
private const val FadedAlpha = 0.4f
private const val MinGappedBarPx = 3f
private val LabelGap = 4.dp
