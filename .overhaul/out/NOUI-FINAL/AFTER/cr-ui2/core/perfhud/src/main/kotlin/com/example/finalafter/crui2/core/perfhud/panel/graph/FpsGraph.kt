package com.example.finalafter.crui2.core.perfhud.panel.graph

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
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.perfhud.R
import com.example.finalafter.crui2.core.perfhud.frame.FpsSeries
import com.example.finalafter.crui2.core.perfhud.frame.FrameLog
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Frames per second while drawing, one bar per second for the last [Seconds], with the display's
 * rate as a line. A second with nothing to draw has no bar: an idle app is not a slow one.
 */
@Composable
internal fun FpsGraph(frames: FrameLog, refreshHz: () -> Float, version: () -> Int, modifier: Modifier = Modifier) {
    val series = remember { FpsSeries(Seconds) }
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.caption1
    val hzFormat = stringResource(R.string.perfhud_hz)
    val colors = AppTheme.colors
    val good = colors.success.content
    val fair = colors.warning.content
    val poor = colors.danger.content
    val guide = colors.contentTertiary
    val axis = colors.separator

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphDefaults.Height)
            .drawWithCache {
                val dash = PathEffect.dashPathEffect(floatArrayOf(DashPx, DashPx))
                val stroke = GraphDefaults.Stroke.toPx()
                // The rate changes only with the display's, which hands this modifier a new lambda.
                val refresh = refreshHz()
                val rateLabel = if (refresh > 0f) {
                    measurer.measure(String.format(Locale.ROOT, hzFormat, Math.round(refresh)), labelStyle)
                } else {
                    null
                }
                onDrawBehind {
                    version()
                    frames.copyFramesPerSecond(System.nanoTime(), series)
                    val top = max(refresh, MinScaleHz) * Headroom
                    val slot = size.width / Seconds
                    for (second in 0 until Seconds) {
                        val fps = series.perSecond[second]
                        if (fps == FpsSeries.NONE) continue
                        val height = min(fps / top, 1f) * size.height
                        val color = when {
                            refresh <= 0f || fps >= refresh * GoodShare -> good
                            fps >= refresh * FairShare -> fair
                            else -> poor
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(second * slot, size.height - height),
                            size = Size(slot * BarShare, height),
                        )
                    }
                    if (rateLabel != null) {
                        val y = size.height * (1f - refresh / top)
                        drawLine(guide, Offset(0f, y), Offset(size.width, y), stroke, pathEffect = dash)
                        drawText(rateLabel, color = guide, topLeft = Offset(0f, y - rateLabel.size.height))
                    }
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
            },
    )
}

private const val Seconds = 60
private const val MinScaleHz = 60f
private const val Headroom = 1.15f
private const val GoodShare = 0.95f
private const val FairShare = 0.75f
private const val BarShare = 0.75f
private const val DashPx = 8f
