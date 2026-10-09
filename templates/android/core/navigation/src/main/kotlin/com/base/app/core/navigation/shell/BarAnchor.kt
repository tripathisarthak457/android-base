package com.base.app.core.navigation.shell

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.LayoutDirection

/**
 * How far a destination that hides the tab bar (a `FullScreenKey`, or any push with `hideBarOnPush`)
 * has slid over the screen beneath it, the last one that showed the bar: 0 with that screen in
 * view, 1 with it covered. The bar reads it to travel with that screen, as
 * `hidesBottomBarWhenPushed` does on iOS, so it leaves and returns with the screen it belongs to,
 * follows a back swipe, and never animates on a clock of its own.
 *
 * The scene with the covering screen on top publishes its progress while it is composed; the last
 * value is kept after it leaves, so the bar does not jump in the frame between a stack change and
 * the scene that animates it.
 */
@Stable
internal class BarAnchor {
    private var follower by mutableStateOf<(() -> Float)?>(null)
    private var last = 0f

    internal fun progress(): Float = follower?.invoke() ?: last

    internal fun attach(progress: () -> Float) {
        follower = progress
    }

    internal fun detach(progress: () -> Float) {
        if (follower !== progress) return
        last = progress()
        follower = null
    }
}

/**
 * Moves the bar with the screen it belongs to (see [BarAnchor]): shifted by that screen's parallax,
 * clipped at the covering screen's leading edge, and darkened by the same scrim that screen gets.
 * All of it in the draw phase, so a push or pop recomposes nothing here. The scrim needs the bar's
 * own pixels as a mask (a floating capsule has transparent margins the screen's scrim already
 * darkens), so only while the bar is partly covered is it drawn through a layer of its own size.
 */
internal fun Modifier.followCovered(anchor: BarAnchor, parallax: Float, scrimAlpha: Float): Modifier =
    this then FollowCoveredElement(anchor, parallax, scrimAlpha)

private data class FollowCoveredElement(
    val anchor: BarAnchor,
    val parallax: Float,
    val scrimAlpha: Float,
) : ModifierNodeElement<FollowCoveredNode>() {
    override fun create() = FollowCoveredNode(anchor, parallax, scrimAlpha)

    override fun update(node: FollowCoveredNode) {
        node.anchor = anchor
        node.parallax = parallax
        node.scrimAlpha = scrimAlpha
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "followCovered"
    }
}

private class FollowCoveredNode(
    var anchor: BarAnchor,
    var parallax: Float,
    var scrimAlpha: Float,
) : Modifier.Node(), DrawModifierNode, LayoutAwareModifierNode {

    private var windowLeft = 0f
    private var windowWidth = 0f
    private val layerPaint = Paint()

    override fun onPlaced(coordinates: LayoutCoordinates) {
        windowLeft = coordinates.positionInWindow().x
        var root = coordinates
        while (true) root = root.parentLayoutCoordinates ?: break
        windowWidth = root.size.width.toFloat()
    }

    override fun ContentDrawScope.draw() {
        val progress = anchor.progress()
        if (progress <= 0f) {
            drawContent()
            return
        }
        val ltr = layoutDirection == LayoutDirection.Ltr
        val shift = parallax * windowWidth * progress * if (ltr) -1f else 1f
        // The part of the window the covering screen has not reached yet, in this node's coordinates.
        val edge = windowWidth * (1f - progress)
        val visibleLeft = if (ltr) -windowLeft else windowWidth - edge - windowLeft
        val visibleRight = if (ltr) edge - windowLeft else windowWidth - windowLeft
        clipRect(left = visibleLeft, right = visibleRight) {
            drawIntoCanvas { canvas -> canvas.saveLayer(Rect(Offset.Zero, size), layerPaint) }
            translate(left = shift) {
                this@draw.drawContent()
                drawRect(Color.Black, size = size, alpha = scrimAlpha * progress, blendMode = BlendMode.SrcAtop)
            }
            drawIntoCanvas { canvas -> canvas.restore() }
        }
    }
}
