package com.example.everything.core.designsystem.foundation.draw

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection

/** The bones of one placeholder, and how they are drawn. */
internal interface Bones {
    /**
     * Draws the bones with [brush], each moved [dx] along x. [lineRadius] is the corner radius of a
     * bone that stands for a line of text, in pixels; a line never rounds by more than half its height.
     */
    fun DrawScope.draw(brush: Brush, dx: Float, lineRadius: Float)
}

/** One bone in [shape], over the whole element. */
internal class OutlineBones(private val shape: Shape) : Bones {
    private var outline: Outline? = null
    private var outlineSize = Size.Unspecified
    private var outlineDirection = LayoutDirection.Ltr
    private var outlineDensity = 0f
    private var path: Path? = null
    private var moved: Path? = null

    override fun DrawScope.draw(brush: Brush, dx: Float, lineRadius: Float) {
        when (val outline = outlineFor(this)) {
            is Outline.Rectangle -> drawRect(brush, topLeft = Offset(dx, 0f), size = size)
            is Outline.Rounded -> {
                val path = path
                if (path == null) {
                    drawRoundRect(brush, Offset(dx, 0f), size, outline.roundRect.topLeftCornerRadius)
                } else {
                    drawMoved(path, brush, dx)
                }
            }
            is Outline.Generic -> drawMoved(outline.path, brush, dx)
        }
    }

    private fun DrawScope.drawMoved(path: Path, brush: Brush, dx: Float) {
        val moved = moved ?: Path().also { moved = it }
        moved.reset()
        moved.addPath(path, Offset(dx, 0f))
        drawPath(moved, brush)
    }

    private fun outlineFor(scope: DrawScope): Outline {
        val cached = outline
        if (cached != null &&
            outlineSize == scope.size &&
            outlineDirection == scope.layoutDirection &&
            outlineDensity == scope.density
        ) {
            return cached
        }
        return shape.createOutline(scope.size, scope.layoutDirection, scope).also {
            outline = it
            outlineSize = scope.size
            outlineDirection = scope.layoutDirection
            outlineDensity = scope.density
            // drawRoundRect takes one corner radius, so a shape whose corners differ is drawn as its path.
            path = if (it is Outline.Rounded && !it.roundRect.isSimple) Path().apply { addOutline(it) } else null
        }
    }
}

/** One line's bone, [top] to [bottom] pixels down the element and as wide as it. */
internal class LineBones(private val top: Float, private val bottom: Float) : Bones {
    override fun DrawScope.draw(brush: Brush, dx: Float, lineRadius: Float) {
        val height = bottom - top
        drawRoundRect(brush, Offset(dx, top), Size(size.width, height), lineCorner(lineRadius, height))
    }
}

/**
 * The lines of a piece of text as bones, kept from its last layout: each from the line's left to its
 * right edge, and from a capital's height above its baseline to a little below it, so a redacted
 * paragraph keeps the rhythm of the text it stands for, at every font scale.
 */
internal class TextBones : Bones {
    /** Left, top, right and bottom of each line, in that order. */
    var lines = FloatArray(0)
        private set

    /** How far down the element [line]'s bone starts. */
    fun top(line: Int): Float = lines[line * 4 + 1]

    /** How far down the element [line]'s bone ends. */
    fun bottom(line: Int): Float = lines[line * 4 + 3]

    fun update(layout: TextLayoutResult) {
        val count = layout.lineCount
        if (lines.size != count * 4) lines = FloatArray(count * 4)
        val em = layout.emPx()
        for (line in 0 until count) {
            val baseline = layout.getLineBaseline(line)
            lines[line * 4] = layout.getLineLeft(line)
            lines[line * 4 + 1] = baseline - em * CapHeight
            lines[line * 4 + 2] = layout.getLineRight(line)
            lines[line * 4 + 3] = baseline + em * BelowBaseline
        }
    }

    override fun DrawScope.draw(brush: Brush, dx: Float, lineRadius: Float) {
        val lines = lines
        var index = 0
        while (index < lines.size) {
            val left = lines[index]
            val top = lines[index + 1]
            val right = lines[index + 2]
            val bottom = lines[index + 3]
            index += 4
            // An empty line stands for no text. A bone's layer does not clip, so a line that maxLines or a
            // fixed height cuts off would draw over whatever lies below the box.
            if (right <= left || top >= size.height) continue
            val height = minOf(bottom, size.height) - top
            drawRoundRect(brush, Offset(left + dx, top), Size(right - left, height), lineCorner(lineRadius, height))
        }
    }

    private fun TextLayoutResult.emPx(): Float {
        val fontSize = layoutInput.style.fontSize
        if (fontSize.isSp) return with(layoutInput.density) { fontSize.toPx() }
        return (getLineBottom(0) - getLineTop(0)) * EmOfLine
    }

    internal companion object {
        /** How far above the baseline a line's bone starts, in ems: about the height of a capital. */
        const val CapHeight = 0.72f

        /** How far below the baseline it ends: enough to suggest descenders without reaching the next line. */
        const val BelowBaseline = 0.1f

        /** The share of a line box one em takes in the type scale, for a style sized in ems. */
        private const val EmOfLine = 0.8f
    }
}

private fun lineCorner(radius: Float, height: Float) = CornerRadius(minOf(radius, height / 2))
