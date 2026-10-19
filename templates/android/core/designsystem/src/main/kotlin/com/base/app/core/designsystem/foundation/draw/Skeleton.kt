package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
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
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.foundation.layout.LocalTabVisible
import com.base.app.core.designsystem.theme.LocalAppColors
import com.base.app.core.designsystem.theme.LocalAppShapes
import com.base.app.core.designsystem.theme.LocalReduceMotion
import com.base.app.core.designsystem.theme.tokens.DesignTokens.Motion

/**
 * Draws this element, and everything inside it, as one placeholder bone in [shape].
 *
 * The window's shimmer sweeps one band across every bone on screen, so a list of placeholders
 * lights up as one surface rather than as a row of separate flickers. The bone holds still with
 * motion reduced, in a hidden tab, in a preview, and outside `AppTheme`. Inside an `AppRedacted`,
 * text, icons, avatars, chips, badges and buttons with a fill draw themselves this way; any other
 * component reads [LocalRedacted] and adds this modifier.
 */
fun Modifier.skeleton(shape: Shape): Modifier = layered(OutlineSkeletonElement(shape))

/**
 * Makes this element the area the shimmer crosses for the bones inside it, instead of the window:
 * for a sheet or a pane whose placeholders should sweep on their own.
 */
fun Modifier.shimmerBounds(): Modifier = this then ShimmerBoundsElement

/**
 * True inside an active `AppRedacted`. A component that does not redact itself draws [skeleton] in
 * place of its content while it is true, and a control that is not built on `Modifier.pressable`
 * takes no input.
 */
val LocalRedacted: CompositionLocal<Boolean> get() = LocalRedactedState

internal val LocalRedactedState = compositionLocalOf { false }

/** A bone [top] to [bottom] pixels down this element and as wide as it: one line of placeholder text. */
internal fun Modifier.lineSkeleton(top: Float, bottom: Float): Modifier =
    layered(LineSkeletonElement(top, bottom))

/** One bone over each line [bones] was last laid out with, in place of the text itself. */
internal fun Modifier.textSkeleton(bones: TextBones): Modifier = layered(TextSkeletonElement(bones))

/**
 * Hosts the [content] of a component that draws one bone over itself while [bone] is true, and keeps
 * that content from drawing bones of its own then: the component's bone never draws what is under
 * it, so those bones would be redrawn on every frame of a sweep and never seen. The provider stays
 * in place while [bone] is false, so redacting the component keeps its content's composition.
 */
@Composable
internal fun ContentUnderBone(bone: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRedactedState provides (!bone && LocalRedactedState.current), content = content)
}

// A sweep frame redraws only the bone: in its own layer, the card or row around it is not re-recorded.
private fun Modifier.layered(element: Modifier.Element): Modifier = graphicsLayer() then element

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
 *
 * It keeps auto-invalidation, unlike the advice for most nodes: the colours and shapes it draws with
 * are static locals, and their change reaches a node only through it.
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
        val sweeps = currentValueOf(LocalTabVisible).isVisible &&
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
