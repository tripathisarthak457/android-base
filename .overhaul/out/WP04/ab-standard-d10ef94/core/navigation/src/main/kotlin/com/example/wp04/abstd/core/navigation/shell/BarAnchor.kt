package com.example.wp04.abstd.core.navigation.shell

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * `hidesBottomBarWhenPushed` does on iOS, so it leaves and returns with the screen it belongs to
 * and never animates on a clock of its own.
 *
 * During a back gesture over such a destination, the screen that shows the bar is revealed at
 * rest under the shrinking card, and so is the bar: [cutout] says where the card is, and the bar is
 * drawn around it, under the card's scrim.
 *
 * The stack publishes both while it is composed; the last progress is kept after it leaves, so the
 * bar does not jump in the frame between a stack change and the scene that animates it.
 */
@Stable
internal class BarAnchor {
    private var follower by mutableStateOf<(() -> Float)?>(null)
    private var card: ((CardCutout) -> Boolean)? = null
    private var last = 0f

    internal fun progress(): Float = follower?.invoke() ?: last

    /** Writes the back card's outline into [into] while a card lifts the bar's screen; false otherwise. */
    internal fun cutout(into: CardCutout): Boolean = card?.invoke(into) ?: false

    internal fun attach(progress: () -> Float, cutout: (CardCutout) -> Boolean) {
        follower = progress
        card = cutout
    }

    internal fun detach(progress: () -> Float) {
        if (follower !== progress) return
        last = progress()
        follower = null
        card = null
    }
}

/** The back card's outline in window px, and how far its scrim and its fade have gone. */
internal class CardCutout {
    var left = 0f
    var top = 0f
    var right = 0f
    var bottom = 0f
    var corner = 0f
    var scrim = 0f

    /** The card's own opacity: on commit it fades, and the bar under it fades in. */
    var opacity = 1f
}

/**
 * Moves the bar, and the edge fade under it, with the screen they belong to (see [BarAnchor]):
 * shifted by that screen's parallax, clipped at the covering screen's leading edge, and darkened by
 * the same scrim that screen gets. All of it in the draw phase, so a push or pop recomposes nothing
 * here. The scrim needs the node's own pixels as a mask (a floating capsule has transparent margins
 * the screen's scrim already darkens), so only while it is partly covered is it drawn through a
 * layer of its own size.
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
    private var windowTop = 0f
    private var windowWidth = 0f
    private val layerPaint = Paint()
    private var layerBounds = Rect.Zero
    private val cutout = CardCutout()
    private val card = Path()

    override fun onPlaced(coordinates: LayoutCoordinates) {
        val position = coordinates.positionInWindow()
        windowLeft = position.x
        windowTop = position.y
        var root = coordinates
        while (true) root = root.parentLayoutCoordinates ?: break
        windowWidth = root.size.width.toFloat()
    }

    override fun ContentDrawScope.draw() {
        if (anchor.cutout(cutout)) {
            drawAroundCard()
            return
        }
        val progress = anchor.progress()
        if (progress <= 0f) {
            drawContent()
            return
        }
        if (progress >= 1f) return
        val ltr = layoutDirection == LayoutDirection.Ltr
        val shift = parallax * windowWidth * progress * if (ltr) -1f else 1f
        // The part of the window the covering screen has not reached yet, in this node's coordinates.
        val edge = windowWidth * (1f - progress)
        val visibleLeft = if (ltr) -windowLeft else windowWidth - edge - windowLeft
        val visibleRight = if (ltr) edge - windowLeft else windowWidth - windowLeft
        clipRect(left = visibleLeft, right = visibleRight) {
            drawIntoCanvas { canvas -> canvas.saveLayer(boundsOf(size), layerPaint) }
            translate(left = shift) {
                this@draw.drawContent()
                drawRect(Color.Black, size = size, alpha = scrimAlpha * progress, blendMode = BlendMode.SrcAtop)
            }
            drawIntoCanvas { canvas -> canvas.restore() }
        }
    }

    /**
     * The bar at rest under the back card: around the card, darkened by its scrim through a layer
     * of the bar's size, and fading in where the card fades out on commit.
     */
    private fun ContentDrawScope.drawAroundCard() {
        card.setRoundedRect(
            cutout.left - windowLeft,
            cutout.top - windowTop,
            cutout.right - windowLeft,
            cutout.bottom - windowTop,
            cutout.corner,
        )
        clipPath(card, ClipOp.Difference) { this@drawAroundCard.drawDimmed(cutout.scrim, opacity = 1f) }
        if (cutout.opacity < 1f) {
            clipPath(card) { this@drawAroundCard.drawDimmed(cutout.scrim, opacity = 1f - cutout.opacity) }
        }
    }

    /** The bar-sized layer's bounds, made again only when the bar's size changes. */
    private fun boundsOf(size: Size): Rect {
        if (layerBounds.width != size.width || layerBounds.height != size.height) layerBounds = Rect(Offset.Zero, size)
        return layerBounds
    }

    private fun ContentDrawScope.drawDimmed(scrim: Float, opacity: Float) {
        layerPaint.alpha = opacity
        drawIntoCanvas { canvas -> canvas.saveLayer(boundsOf(size), layerPaint) }
        this@drawDimmed.drawContent()
        if (scrim > 0f) drawRect(Color.Black, size = size, alpha = scrim, blendMode = BlendMode.SrcAtop)
        drawIntoCanvas { canvas -> canvas.restore() }
        layerPaint.alpha = 1f
    }
}

/** A rounded rectangle from lines and cubic quarter arcs, rewritten in place: no allocation per frame. */
private fun Path.setRoundedRect(left: Float, top: Float, right: Float, bottom: Float, radius: Float) {
    rewind()
    if (right <= left || bottom <= top) return
    val r = radius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
    val k = r * KAPPA
    moveTo(left + r, top)
    lineTo(right - r, top)
    cubicTo(right - r + k, top, right, top + r - k, right, top + r)
    lineTo(right, bottom - r)
    cubicTo(right, bottom - r + k, right - r + k, bottom, right - r, bottom)
    lineTo(left + r, bottom)
    cubicTo(left + r - k, bottom, left, bottom - r + k, left, bottom - r)
    lineTo(left, top + r)
    cubicTo(left, top + r - k, left + r - k, top, left + r, top)
    close()
}

private const val KAPPA = 0.5522848f
