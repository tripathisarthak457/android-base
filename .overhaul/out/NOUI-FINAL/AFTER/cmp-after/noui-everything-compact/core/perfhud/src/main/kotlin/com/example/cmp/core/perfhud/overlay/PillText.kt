package com.example.cmp.core.perfhud.overlay

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * The pill's text: its lines written into buffers of their own ([LineFormat]) and drawn with one
 * paint, kept for as long as the formats, the style and the density hold.
 *
 * A redraw lands inside a frame the app is drawing, often a scroll's, so it formats no string,
 * boxes no number and lays out no text: Compose's text layout would build a new layout for every
 * new number, since the numbers are what change. [width] and [height] are those of the widest
 * line the formats can make, so a changing number never resizes the pill.
 */
internal class PillText(
    fpsFormat: String,
    idleFormat: String,
    jankyFormat: String,
    style: TextStyle,
    typeface: Typeface?,
    density: Density,
) {
    private val fps = LineFormat(fpsFormat)
    private val idle = LineFormat(idleFormat)
    private val janky = LineFormat(jankyFormat)

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize =
            with(density) {
                style.fontSize.takeIf { it.type == TextUnitType.Sp }?.toPx()
                    ?: FallbackSize.toPx()
            }
        letterSpacing = when (style.letterSpacing.type) {
            TextUnitType.Em -> style.letterSpacing.value
            TextUnitType.Sp -> with(density) { style.letterSpacing.toPx() } / textSize
            else -> 0f
        }
        style.fontFeatureSettings?.let { fontFeatureSettings = it }
    }

    private val lineHeight: Float
    private val baseline: Float

    val width: Float
    val height: Float

    init {
        val metrics = paint.fontMetrics
        val glyphs = metrics.descent - metrics.ascent
        lineHeight = maxOf(
            glyphs,
            with(density) { style.lineHeight.takeIf { it.type == TextUnitType.Sp }?.toPx() } ?: 0f,
        )
        // Centred in its line as Compose centres text in a taller line height.
        baseline = (lineHeight - glyphs) / 2f - metrics.ascent
        fps.write(WidestNumber, WidestNumber)
        val fpsWidth = measured(fps)
        idle.write(WidestNumber)
        val idleWidth = measured(idle)
        janky.write(WidestPercent, 0)
        width = ceil(maxOf(fpsWidth, idleWidth, measured(janky)))
        height = ceil(lineHeight * 2)
    }

    /** The first line: [framesPerSecond] against [refreshHz], or idle at [refreshHz]. */
    fun DrawScope.drawRate(framesPerSecond: Int, refreshHz: Int, isIdle: Boolean, color: Color) {
        val line = if (isIdle) {
            idle.also {
                it.write(refreshHz)
            }
        } else {
            fps.also { it.write(framesPerSecond, refreshHz) }
        }
        drawCentred(line, color, top = 0f)
    }

    /** The second line: [jankyTenths] tenths of a percent janky. */
    fun DrawScope.drawJanky(jankyTenths: Int, color: Color) {
        janky.write(jankyTenths / TenthsPerUnit, jankyTenths % TenthsPerUnit)
        drawCentred(janky, color, top = lineHeight)
    }

    private fun DrawScope.drawCentred(line: LineFormat, color: Color, top: Float) {
        paint.color = color.toArgb()
        val x = (size.width - measured(line)) / 2f
        drawContext.canvas.nativeCanvas.drawText(
            line.chars,
            0,
            line.length,
            x,
            top + baseline,
            paint,
        )
    }

    private fun measured(line: LineFormat): Float = paint.measureText(line.chars, 0, line.length)
}

private const val WidestNumber = 888
private const val WidestPercent = 100
private const val TenthsPerUnit = 10
private val FallbackSize = 11.sp
