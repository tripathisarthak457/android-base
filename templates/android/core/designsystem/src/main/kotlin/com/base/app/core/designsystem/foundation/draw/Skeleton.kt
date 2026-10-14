package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.theme.LocalAppColors
import com.base.app.core.designsystem.theme.LocalAppShapes
import com.base.app.core.designsystem.theme.LocalReduceMotion
import com.base.app.core.designsystem.theme.tokens.DesignTokens.Motion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draws this element, and everything inside it, as one placeholder bone in [shape].
 *
 * The window's shimmer sweeps one band across every bone on screen, so a list of placeholders
 * lights up as one surface rather than as a row of separate flickers. The bone holds still with
 * motion reduced, in a hidden tab, in a preview, and outside `AppTheme`. Inside an `AppRedacted`,
 * the design system's components draw themselves this way; a custom component reads
 * [LocalRedacted] and adds this modifier.
 */
fun Modifier.skeleton(shape: Shape): Modifier = layered(OutlineSkeletonElement(shape))

/**
 * Makes this element the area the shimmer crosses for the bones inside it, instead of the window:
 * for a sheet or a pane whose placeholders should sweep on their own.
 */
fun Modifier.shimmerBounds(): Modifier = this then ShimmerBoundsElement

internal val LocalRedactedState = compositionLocalOf { false }

/** True inside an active `AppRedacted`: a custom component draws [skeleton] in place of its content. */
val LocalRedacted: CompositionLocal<Boolean> = LocalRedactedState

/** A bone [top] to [bottom] pixels down this element and as wide as it: one line of placeholder text. */
internal fun Modifier.lineSkeleton(top: Float, bottom: Float): Modifier =
    layered(LineSkeletonElement(top, bottom))

/** One bone over each line [bones] was last laid out with, in place of the text itself. */
internal fun Modifier.textSkeleton(bones: TextBones): Modifier = layered(TextSkeletonElement(bones))

// A sweep frame redraws only the bone: in its own layer, the card or row around it is not re-recorded.
private fun Modifier.layered(element: Modifier.Element): Modifier = graphicsLayer() then element

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
            // One radius for every corner is one round rect; any other rounding is drawn as its path.
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
            // An empty line has no bone, and a line the box cuts off is not drawn past its edge.
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

private const val TiltRadians = Motion.skeletonTiltDegrees * PI / 180

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

private object ShimmerBoundsKey

private data object ShimmerBoundsElement : ModifierNodeElement<ShimmerBoundsNode>() {
    override fun create() = ShimmerBoundsNode()

    override fun update(node: ShimmerBoundsNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "shimmerBounds"
    }
}

private class ShimmerBoundsNode : Modifier.Node(), TraversableNode, LayoutAwareModifierNode {
    override val traverseKey: Any get() = ShimmerBoundsKey

    var coordinates: LayoutCoordinates? = null
        private set

    override fun onPlaced(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }

    override fun onDetach() {
        coordinates = null
    }
}

private data class OutlineSkeletonElement(val shape: Shape) : ModifierNodeElement<SkeletonNode>() {
    override fun create() = SkeletonNode(OutlineBones(shape))

    override fun update(node: SkeletonNode) = node.update(OutlineBones(shape))

    override fun InspectorInfo.inspectableProperties() {
        name = "skeleton"
        properties["shape"] = shape
    }
}

private data class LineSkeletonElement(
    val top: Float,
    val bottom: Float,
) : ModifierNodeElement<SkeletonNode>() {
    override fun create() = SkeletonNode(LineBones(top, bottom))

    override fun update(node: SkeletonNode) = node.update(LineBones(top, bottom))

    override fun InspectorInfo.inspectableProperties() {
        name = "lineSkeleton"
        properties["top"] = top
        properties["bottom"] = bottom
    }
}

private data class TextSkeletonElement(val bones: TextBones) : ModifierNodeElement<SkeletonNode>() {
    override fun create() = SkeletonNode(bones)

    override fun update(node: SkeletonNode) = node.update(bones)

    override fun InspectorInfo.inspectableProperties() {
        name = "textSkeleton"
    }
}

/**
 * A placeholder: its [bones] drawn plain while the shimmer rests, and while it sweeps, with the one
 * band of the sweep area across them. The content is never drawn.
 *
 * A node joins the window's [ShimmerClock] only while it may move (on screen, motion allowed, not a
 * preview). Those conditions are observed, so a tab switch or the motion setting changes what the
 * bone does without recomposing anything. A sweep frame costs a bone one draw into its own layer,
 * and allocates nothing: the gradient is built once per colour, band width and direction, and moved
 * by translating the canvas.
 */
private class SkeletonNode(private var bones: Bones) :
    Modifier.Node(),
    DrawModifierNode,
    CompositionLocalConsumerModifierNode,
    ObserverModifierNode,
    ShimmerClock.Bone {

    private var clock: ShimmerClock? = null
    private var area: ShimmerBoundsNode? = null
    private var root: LayoutCoordinates? = null
    private var plain: SolidColor? = null
    private var sweep: SweepBrush? = null

    fun update(bones: Bones) {
        this.bones = bones
        invalidateDraw()
    }

    override fun onAttach() {
        area = findNearestAncestor(ShimmerBoundsKey) as ShimmerBoundsNode?
        follow()
    }

    override fun onObservedReadsChanged() = follow()

    override fun onDetach() {
        clock?.remove(this)
        clock = null
        area = null
        root = null
    }

    override fun onSweepFrame() = invalidateDraw()

    private fun follow() = observeReads {
        val sweeps = currentValueOf(LocalTabVisible) &&
            !currentValueOf(LocalReduceMotion).value &&
            !currentValueOf(LocalInspectionMode)
        val next = if (sweeps) currentValueOf(LocalShimmerClock) else null
        if (next !== clock) {
            clock?.remove(this)
            clock = next
            next?.add(this)
            invalidateDraw()
        }
    }

    override fun ContentDrawScope.draw() {
        val colors = currentValueOf(LocalAppColors)
        val lineRadius = lineRadius()
        val progress = clock?.progress ?: Float.NaN
        if (progress.isNaN()) {
            with(bones) { draw(plainBrush(colors.skeleton), 0f, lineRadius) }
            return
        }
        val self = requireLayoutCoordinates()
        val bounds = area?.coordinates?.takeIf { it.isAttached }
        val space = bounds ?: root ?: self.findRootCoordinates().also { root = it }
        val origin = if (bounds != null) bounds.localPositionOf(self, Offset.Zero) else self.positionInRoot()
        val rtl = layoutDirection == LayoutDirection.Rtl
        drawSwept(
            bones = bones,
            brush = sweepBrush(colors.skeleton, colors.skeletonHighlight, Motion.skeletonBandWidth.toPx(), rtl),
            progress = progress,
            x = origin.x,
            y = origin.y,
            width = space.size.width.toFloat(),
            height = space.size.height.toFloat(),
            lineRadius = lineRadius,
        )
    }

    /** The style's smallest corner, which bones that stand for text are rounded by. */
    private fun DrawScope.lineRadius(): Float {
        val xs = currentValueOf(LocalAppShapes).xs as? CornerBasedShape ?: return 0f
        return xs.topStart.toPx(size, this)
    }

    private fun plainBrush(bone: Color): SolidColor {
        val current = plain
        if (current != null && current.value == bone) return current
        return SolidColor(bone).also { plain = it }
    }

    private fun sweepBrush(bone: Color, highlight: Color, band: Float, rtl: Boolean): SweepBrush {
        val current = sweep
        if (current != null && current.matches(bone, highlight, band, rtl)) return current
        return SweepBrush(bone, highlight, band, rtl).also { sweep = it }
    }
}
