package com.example.hud.fixture.core.perfhud.panel.graph

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
import com.example.hud.fixture.core.designsystem.theme.AppTheme
import com.example.hud.fixture.core.perfhud.R
import com.example.hud.fixture.core.perfhud.frame.FrameLog
import com.example.hud.fixture.core.perfhud.frame.FrameSeries
import java.util.Locale
import kotlin.math.min

/**
 * The last frames as bars, against the frame budget at 60, 90 and 120 Hz: green inside the
 * display's budget, amber over it, red for a frame that missed its deadline.
 *
 * Drawn from [frames] in the draw phase each time [version] changes, into a series allocated once.
 */
@Composable
internal fun FrameTimeGraph(frames: FrameLog, version: () -> Int, modifier: Modifier = Modifier) {
    val series = remember { FrameSeries(BARS) }
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.caption
    val hzFormat = stringResource(R.string.perfhud_hz)
    val colors = AppTheme.colors
    val good = colors.success.content
    val over = colors.warning.content
    val missed = colors.danger.content
    val guide = colors.contentTertiary
    val axis = colors.divider

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphDefaults.Height)
            .drawWithCache {
                val labels = BUDGET_HZ.map { hz ->
                    measurer.measure(String.format(Locale.ROOT, hzFormat, hz), labelStyle)
                }
                val dash = PathEffect.dashPathEffect(floatArrayOf(DASH_PX, DASH_PX))
                val stroke = GraphDefaults.Stroke.toPx()
                onDrawBehind {
                    version()
                    frames.copyRecent(series)
                    val barWidth = size.width / BARS
                    val gap = if (barWidth > MIN_GAPPED_BAR_PX) 1f else 0f
                    val start = size.width - series.count * barWidth
                    for (i in 0 until series.count) {
                        val total = series.totalMillis[i]
                        val height = min(total / SCALE_MILLIS, 1f) * size.height
                        val color = when {
                            series.janky[i] -> missed
                            total > series.intervalMillis[i] -> over
                            else -> good
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(start + i * barWidth, size.height - height),
                            size = Size(barWidth - gap, height),
                        )
                    }
                    for (index in BUDGET_HZ.indices) {
                        val y = size.height * (1f - MILLIS_PER_SECOND / BUDGET_HZ[index] / SCALE_MILLIS)
                        drawLine(guide, Offset(0f, y), Offset(size.width, y), stroke, pathEffect = dash)
                        val label = labels[index]
                        drawText(label, color = guide, topLeft = Offset(0f, y - label.size.height))
                    }
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
            },
    )
}

/** About two seconds of drawing at 120 Hz, each bar a little over a pixel wide on a phone. */
private const val BARS = 240

/** The graph's top: two frames at 60 Hz. A taller frame is drawn to the top. */
private const val SCALE_MILLIS = 33.3f

private val BUDGET_HZ = intArrayOf(60, 90, 120)
private const val MILLIS_PER_SECOND = 1000f
private const val DASH_PX = 8f
private const val MIN_GAPPED_BAR_PX = 3f
