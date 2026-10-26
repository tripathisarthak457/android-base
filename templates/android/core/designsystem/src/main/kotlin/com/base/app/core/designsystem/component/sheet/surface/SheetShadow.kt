package com.base.app.core.designsystem.component.sheet.surface

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * The geometry of a nine-slice shadow: a shadow drawn once around a small round rect, then
 * stretched around a surface of any size. Corners never scale; edges stretch along their axis; the
 * centre is never drawn, because the interior was cleared, so a translucent body can never show it.
 */
internal class NineSlice(
    /** How far the shadow reaches past the rect on every side: blur, spread and offset. */
    val padding: Int,
    /** The corner radius in pixels, rounded up. */
    val corner: Int,
) {
    /** The source rect's side: two corners and a two-pixel stretchable middle. */
    val rectSide: Int get() = 2 * corner + MiddlePx

    /** The bitmap's side: the rect and the padding around it. */
    val bitmapSide: Int get() = rectSide + 2 * padding

    /** A corner slice's side, from the bitmap's edge to where the straight edge starts. */
    val cut: Int get() = padding + corner

    /**
     * Writes the destination rect of slice [index] (row-major, 0..8, the centre 4 excluded) around a
     * surface rect into [out] as left, top, width, height. Returns false for the centre and for an
     * edge with nothing to stretch.
     */
    fun destination(
        index: Int,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        out: IntArray,
    ): Boolean {
        if (index == 4) return false
        val outerLeft = left - padding
        val outerTop = top - padding
        val outerRight = right + padding
        val outerBottom = bottom + padding
        val column = index % 3
        val row = index / 3
        val x = when (column) {
            0 -> outerLeft
            1 -> outerLeft + cut
            else -> outerRight - cut
        }
        val width = when (column) {
            1 -> (outerRight - outerLeft) - 2 * cut
            else -> cut
        }
        val y = when (row) {
            0 -> outerTop
            1 -> outerTop + cut
            else -> outerBottom - cut
        }
        val height = when (row) {
            1 -> (outerBottom - outerTop) - 2 * cut
            else -> cut
        }
        if (width <= 0 || height <= 0) return false
        out[0] = x
        out[1] = y
        out[2] = width
        out[3] = height
        return true
    }

    /** Writes the source rect of slice [index] in the bitmap into [out] as left, top, width, height. */
    fun source(index: Int, out: IntArray) {
        val column = index % 3
        val row = index / 3
        out[0] = when (column) {
            0 -> 0
            1 -> cut
            else -> bitmapSide - cut
        }
        out[2] = if (column == 1) MiddlePx else cut
        out[1] = when (row) {
            0 -> 0
            1 -> cut
            else -> bitmapSide - cut
        }
        out[3] = if (row == 1) MiddlePx else cut
    }

    companion object {
        const val MiddlePx = 2

        fun of(shadow: Shadow, corner: Float, density: Density): NineSlice = with(density) {
            val reach = shadow.radius.toPx() + shadow.spread.toPx() +
                maxOf(
                    kotlin.math.abs(shadow.offset.x.toPx()),
                    kotlin.math.abs(shadow.offset.y.toPx()),
                )
            NineSlice(padding = ceil(reach).toInt(), corner = ceil(corner).toInt().coerceAtLeast(1))
        }
    }
}

/**
 * One sheet's shadow: the bitmap made once per key (radius, blur, spread, offset, colour, alpha and
 * density), at most one per theme, and drawn every frame as eight slices around the surface. Nothing
 * is allocated per frame, and nothing is drawn at alpha 0, where the painter would rasterise anyway.
 */
internal class SheetShadowCache {
    private var key: Shadow? = null
    private var keyCorner = 0f
    private var keyDensity = 0f
    private var slice: NineSlice? = null
    private var image: ImageBitmap? = null
    private val source = IntArray(4)
    private val destination = IntArray(4)

    fun DrawScope.drawShadow(
        shadow: Shadow,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        corner: Float,
        alpha: Float,
    ) {
        if (alpha <= 0f || shadow.alpha <= 0f) return
        val slice = prepare(shadow, corner)
        val image = image ?: return
        val l = left.roundToInt()
        val t = top.roundToInt()
        val r = right.roundToInt()
        val b = bottom.roundToInt()
        for (index in 0 until 9) {
            if (!slice.destination(index, l, t, r, b, destination)) continue
            slice.source(index, source)
            drawImage(
                image = image,
                srcOffset = IntOffset(source[0], source[1]),
                srcSize = IntSize(source[2], source[3]),
                dstOffset = IntOffset(destination[0], destination[1]),
                dstSize = IntSize(destination[2], destination[3]),
                alpha = alpha,
            )
        }
    }

    private fun DrawScope.prepare(shadow: Shadow, corner: Float): NineSlice {
        val current = slice
        if (current != null && key == shadow && keyCorner == corner &&
            keyDensity == density
        ) {
            return current
        }
        val made = NineSlice.of(shadow, corner, this)
        val side = made.bitmapSide
        val bitmap = ImageBitmap(side, side)
        val rectSize = Size(made.rectSide.toFloat(), made.rectSide.toFloat())
        val cornerPx = made.corner.toFloat()
        CanvasDrawScope().draw(
            this,
            LayoutDirection.Ltr,
            Canvas(bitmap),
            Size(side.toFloat(), side.toFloat()),
        ) {
            translate(made.padding.toFloat(), made.padding.toFloat()) {
                with(DropShadowPainter(RoundedCornerShape(cornerPx), shadow)) { draw(rectSize) }
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset.Zero,
                    size = rectSize,
                    cornerRadius = CornerRadius(cornerPx),
                    blendMode = BlendMode.Clear,
                )
            }
        }
        key = shadow
        keyCorner = corner
        keyDensity = density
        slice = made
        image = bitmap
        return made
    }
}
