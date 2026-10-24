package com.base.app.core.designsystem.foundation.scroll

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * Content running under floating chrome fades out behind it, so a clear top bar's title and a
 * floating tab bar stay legible without either needing a background of its own: [top] is where
 * the top bar ends and [bottom] how much of the bottom edge the chrome covers, both read while
 * drawing.
 *
 * It draws [color], the solid background the content sits on, over the content as a gradient,
 * which gives the same pixels as fading the content out and costs one rect per edge: no offscreen
 * layer. Over a background that is not one colour (glass, an image) use [scrollEdgeFade].
 */
fun Modifier.edgeProtection(
    color: ColorProducer,
    top: () -> Dp = { 0.dp },
    bottom: () -> Dp = { 0.dp },
): Modifier = drawWithCache {
    val solid = color()
    val topEdge = top().toPx().coerceIn(0f, size.height)
    val bottomEdge = bottom().toPx().coerceIn(0f, size.height)
    val tail = FadeTail.toPx()
    val topEnd = (topEdge + tail).coerceAtMost(size.height)
    val bottomEnd = (bottomEdge + tail).coerceAtMost(size.height)
    val topBrush = protection(solid, topEdge, topEnd, startY = 0f, endY = topEnd)
    val bottomBrush = protection(solid, bottomEdge, bottomEnd, startY = size.height, endY = size.height - bottomEnd)
    onDrawWithContent {
        drawContent()
        if (topEdge > 0f) drawRect(topBrush, size = Size(size.width, topEnd))
        if (bottomEdge > 0f) drawRect(bottomBrush, topLeft = Offset(0f, size.height - bottomEnd), size = Size(size.width, bottomEnd))
    }
}

/** Opaque under the chrome, a trace of content at its edge, clear by the end of the tail. */
private fun protection(color: Color, edge: Float, end: Float, startY: Float, endY: Float): Brush = Brush.verticalGradient(
    0f to color,
    edge * FadeFrom / end to color,
    edge / end to color.copy(alpha = color.alpha * (1f - TraceAlpha)),
    1f to color.copy(alpha = 0f),
    startY = startY,
    endY = endY,
)

/**
 * Content scrolling up under a clear top bar fades out behind it, so the bar's title and buttons
 * stay legible without the bar needing a background of its own. This version really fades the
 * content, for a bar over a background that is not one colour; on a solid background
 * [edgeProtection] gives the same pixels for less.
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
        edgePx * FadeFrom / end to Color.Transparent,
        edgePx / end to Color.Black.copy(alpha = TraceAlpha),
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

/**
 * A bounded scroller with more below fades its last [FadeTail] out at its bottom edge, so a row the
 * edge cuts reads as "there is more" rather than as clipped. [more] is read while drawing (a list
 * state's `canScrollForward`), so scrolling recomposes nothing; with nothing more below, the content
 * is drawn as it is, with no layer. The fade is real alpha, as [scrollEdgeFade]'s, so it holds over
 * rows of different colours.
 */
internal fun Modifier.bottomEdgeFade(more: () -> Boolean): Modifier = drawWithCache {
    val tail = FadeTail.toPx().coerceAtMost(size.height)
    val start = size.height - tail
    val content = obtainGraphicsLayer()
    val strip = obtainGraphicsLayer().apply { compositingStrategy = CompositingStrategy.Offscreen }
    val mask = Brush.verticalGradient(0f to Color.Black, 1f to Color.Transparent, endY = tail)
    onDrawWithContent {
        if (!more() || tail <= 0f) {
            drawContent()
            return@onDrawWithContent
        }
        content.record { this@onDrawWithContent.drawContent() }
        clipRect(bottom = start) { drawLayer(content) }
        strip.record(IntSize(size.width.toInt(), tail.toInt().coerceAtLeast(1))) {
            translate(top = -start) { drawLayer(content) }
            drawRect(mask, blendMode = BlendMode.DstIn)
        }
        translate(top = start) { drawLayer(strip) }
    }
}

/** How far from the edge content takes to fade in completely. */
private val FadeTail = 32.dp

// Content starts to show this far down the bar, and only faintly by its end.
private const val FadeFrom = 0.7f
private const val TraceAlpha = 0.12f
