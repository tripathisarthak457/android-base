package com.base.app.core.designsystem.foundation

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Content scrolling up under a clear top bar fades out behind it, so the bar's title and buttons
 * stay legible without the bar needing a background of its own.
 *
 * Only a strip at the top pays for it: the content is recorded once per frame and drawn as it is
 * below the strip; the strip is drawn again from the same recording into a small offscreen layer and
 * masked by an alpha ramp that meets the full content with no step. [edge] is where the bar ends,
 * usually `{ LocalTopChrome.current.value }` read through the scaffold; it is read while drawing,
 * so a bar that changes height redraws the strip and recomposes nothing.
 */
fun Modifier.scrollEdgeFade(edge: () -> Dp): Modifier = drawWithCache {
    val edgePx = edge().toPx().coerceIn(0f, size.height)
    if (edgePx <= 0f) return@drawWithCache onDrawWithContent { drawContent() }
    val end = (edgePx + FadeTail.toPx()).coerceAtMost(size.height)
    val content = obtainGraphicsLayer()
    val strip = obtainGraphicsLayer().apply { compositingStrategy = CompositingStrategy.Offscreen }
    val mask = Brush.verticalGradient(
        0f to Color.Transparent,
        edgePx * FADE_FROM / end to Color.Transparent,
        edgePx / end to Color.Black.copy(alpha = TRACE_ALPHA),
        1f to Color.Black,
        endY = end,
    )
    onDrawWithContent {
        content.record { this@onDrawWithContent.drawContent() }
        clipRect(top = end) { drawLayer(content) }
        strip.record(IntSize(size.width.toInt(), end.toInt().coerceAtLeast(1))) {
            drawLayer(content)
            drawRect(mask, blendMode = BlendMode.DstIn)
        }
        drawLayer(strip)
    }
}

/** How far below the bar content takes to fade in completely. */
private val FadeTail = 32.dp

// Content starts to show this far down the bar, and only faintly by its end.
private const val FADE_FROM = 0.7f
private const val TRACE_ALPHA = 0.12f
