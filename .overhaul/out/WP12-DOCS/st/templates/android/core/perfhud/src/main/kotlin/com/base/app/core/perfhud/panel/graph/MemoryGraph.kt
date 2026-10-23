package com.base.app.core.perfhud.panel.graph

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.perfhud.R
import com.base.app.core.perfhud.memory.MemoryLog
import com.base.app.core.perfhud.memory.MemorySeries
import java.util.Locale
import kotlin.math.max

/**
 * The Java heap and the native heap, a point a second for the last [Samples] seconds, and PSS as
 * dots where it was measured. The paths are reused from draw to draw, rewound rather than
 * re-allocated.
 */
@Composable
internal fun MemoryGraph(memory: MemoryLog, version: () -> Int, modifier: Modifier = Modifier) {
    val series = remember { MemorySeries(Samples) }
    val measurer = rememberTextMeasurer()
    val labelStyle = AppTheme.typography.caption1
    val megabytes = stringResource(R.string.perfhud_value_megabytes)
    val colors = AppTheme.colors
    val java = colors.accent
    val native = colors.secondary
    val pss = colors.tertiary
    val axis = colors.separator
    val label = colors.contentTertiary

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(GraphDefaults.Height)
            .drawWithCache {
                val javaPath = Path()
                val nativePath = Path()
                val line = Stroke(width = GraphDefaults.LineWidth.toPx())
                val stroke = GraphDefaults.Stroke.toPx()
                val dot = GraphDefaults.LineWidth.toPx() * DotScale
                // Laid out again only when the scale's whole megabytes change, not on every refresh.
                var scaleMb = -1
                var scale: TextLayoutResult? = null
                onDrawBehind {
                    version()
                    memory.copyRecent(series)
                    if (series.count == 0) return@onDrawBehind
                    var highest = 0f
                    for (i in 0 until series.count) {
                        highest = max(highest, max(series.javaHeapMb[i], series.nativeHeapMb[i]))
                        if (!series.pssMb[i].isNaN()) highest = max(highest, series.pssMb[i])
                    }
                    val top = highest * Headroom
                    val step = size.width / (Samples - 1)
                    val start = size.width - (series.count - 1) * step
                    trace(javaPath, series.javaHeapMb, series.count, start, step, top)
                    trace(nativePath, series.nativeHeapMb, series.count, start, step, top)
                    drawPath(javaPath, java, style = line)
                    drawPath(nativePath, native, style = line)
                    for (i in 0 until series.count) {
                        val value = series.pssMb[i]
                        if (value.isNaN()) continue
                        drawCircle(pss, radius = dot, center = Offset(start + i * step, yOf(value, top)))
                    }
                    drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                    val topMb = Math.round(top)
                    val scaleLayout = scale?.takeIf { topMb == scaleMb }
                        ?: measurer.measure(String.format(Locale.ROOT, megabytes, topMb.toString()), labelStyle)
                    scaleMb = topMb
                    scale = scaleLayout
                    drawText(scaleLayout, color = label, topLeft = Offset.Zero)
                }
            },
    )
}

private fun DrawScope.trace(path: Path, values: FloatArray, count: Int, start: Float, step: Float, top: Float) {
    path.rewind()
    for (i in 0 until count) {
        val x = start + i * step
        val y = yOf(values[i], top)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
}

private fun DrawScope.yOf(value: Float, top: Float): Float = size.height * (1f - value / top)

private const val Samples = 300
private const val Headroom = 1.15f
private const val DotScale = 1.25f
