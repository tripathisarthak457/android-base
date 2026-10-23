package com.example.videoalone.core.designsystem.foundation.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import com.example.videoalone.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The band's lean, as the cosine and sine that turn a point (x, y) into its phase along the sweep. */
internal val TiltCos = cos(TiltRadians).toFloat()
internal val TiltSin = sin(TiltRadians).toFloat()

/**
 * Where the shared band is, for a bone at a given place in the sweep area.
 *
 * A point (x, y) of the area is at phase `x·cos θ + y·sin θ`, θ being the tilt. The band covers the
 * phases from `start` to `start + band`; `start` runs from `-band`, wholly before the area, to the
 * phase of the far corner, wholly past it. A bone lays the band's gradient out from its own origin,
 * so moving the canvas by [shift] and drawing the bone back at `-shift` samples the gradient where
 * the bone really is: every bone in the area draws the same band in the same frame.
 */
internal object SweepGeometry {
    private val TiltTan = TiltSin / TiltCos

    /** The band's leading phase at [progress] of a sweep across an area of [width] by [height]. */
    fun start(progress: Float, width: Float, height: Float, band: Float): Float =
        -band + progress * (width * TiltCos + height * TiltSin + band)

    /**
     * How far to move the canvas for a bone at ([x], [y]) in the area. Right to left, the band enters
     * at the right edge and leans the other way, and the gradient runs leftwards to match.
     */
    fun shift(
        progress: Float,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        band: Float,
        rtl: Boolean,
    ): Float {
        val along = start(progress, width, height, band) / TiltCos
        return if (rtl) (width - x) + y * TiltTan - along else along - x - y * TiltTan
    }

    /** Where the gradient ends, from a bone's origin: one band width along the sweep. */
    fun gradientEnd(band: Float, rtl: Boolean): Offset =
        Offset(if (rtl) -band * TiltCos else band * TiltCos, band * TiltSin)
}

/** The band's gradient from bone to highlight and back, clamped so a bone outside the band is plain. */
internal class SweepBrush(
    val bone: Color,
    val highlight: Color,
    val band: Float,
    val rtl: Boolean,
) : ShaderBrush() {
    private val shader = LinearGradientShader(
        from = Offset.Zero,
        to = SweepGeometry.gradientEnd(band, rtl),
        colors = listOf(bone, highlight, bone),
        colorStops = listOf(0f, 0.5f, 1f),
        tileMode = TileMode.Clamp,
    )

    fun matches(bone: Color, highlight: Color, band: Float, rtl: Boolean): Boolean =
        bone == this.bone && highlight == this.highlight && band == this.band && rtl == this.rtl

    override fun createShader(size: Size): Shader = shader
}

/**
 * One frame of the sweep on [bones]: the band at [progress], for a bone at ([x], [y]) in a sweep area
 * of [width] by [height]. Translating the canvas moves the gradient without building a new one.
 */
internal fun DrawScope.drawSwept(
    bones: Bones,
    brush: SweepBrush,
    progress: Float,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    lineRadius: Float,
) {
    val shift = SweepGeometry.shift(progress, x, y, width, height, brush.band, brush.rtl)
    translate(left = shift) { with(bones) { draw(brush, -shift, lineRadius) } }
}

private const val TiltRadians = Motion.skeletonTiltDegrees * PI / 180
