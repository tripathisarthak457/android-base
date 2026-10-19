package com.base.app.core.designsystem.foundation.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil

/**
 * The glow of a finger on glass, as the Kyant catalog's `InteractiveHighlight` draws it: the whole
 * surface brightens a little, and a soft pool of light sits under the touch, both added to what is
 * beneath. Put it after the surface and before the content: inside the glass's clip, or with
 * [shape], which clips the glow itself.
 *
 * The pool is recorded once per size and moved with a translation, so following a finger sets a
 * layer's alpha and position per frame and allocates nothing; the catalog's draws a runtime shader
 * per frame instead.
 *
 * @param strength 0 to 1, read in draw: how hard the finger presses.
 * @param position where the finger is, in this node's coordinates, read in draw.
 * @param shape clips the glow to the glass; null when the parent already clips.
 */
internal fun Modifier.pressGlow(strength: () -> Float, position: () -> Offset, shape: Shape? = null): Modifier =
    this then PressGlowElement(strength, position, shape)

private data class PressGlowElement(
    val strength: () -> Float,
    val position: () -> Offset,
    val shape: Shape?,
) : ModifierNodeElement<PressGlowNode>() {
    override fun create() = PressGlowNode(strength, position, shape)

    override fun update(node: PressGlowNode) = node.update(strength, position, shape)

    override fun InspectorInfo.inspectableProperties() {
        name = "pressGlow"
    }
}

private class PressGlowNode(
    private var strength: () -> Float,
    private var position: () -> Offset,
    private var shape: Shape?,
) : Modifier.Node(), DrawModifierNode {

    private var pool: GraphicsLayer? = null
    private var poolRadius = 0f

    // The clip, rebuilt only when the node is resized or reshaped.
    private val clip = Path()
    private var clipSize = Size.Unspecified
    private var clipDirection: LayoutDirection? = null
    private var rectangular = false

    fun update(strength: () -> Float, position: () -> Offset, shape: Shape?) {
        this.strength = strength
        this.position = position
        if (shape != this.shape) clipSize = Size.Unspecified
        this.shape = shape
        invalidateDraw()
    }

    override fun onAttach() {
        pool = requireGraphicsContext().createGraphicsLayer().apply { blendMode = BlendMode.Plus }
    }

    override fun onDetach() {
        pool?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        pool = null
        poolRadius = 0f
        clipSize = Size.Unspecified
    }

    override fun ContentDrawScope.draw() {
        val strength = strength().coerceIn(0f, 1f)
        if (strength > 0f) {
            val shape = shape
            if (shape == null) {
                glow(strength)
            } else {
                if (size != clipSize || layoutDirection != clipDirection) reclip(shape)
                if (rectangular) glow(strength) else clipPath(clip) { glow(strength) }
            }
        }
        drawContent()
    }

    private fun DrawScope.glow(strength: Float) {
        drawRect(Color.White, alpha = SheenAlpha * strength, blendMode = BlendMode.Plus)
        val pool = pool ?: return
        val radius = size.minDimension * PoolReach
        if (radius != poolRadius) record(pool, radius)
        pool.alpha = PoolAlpha * strength
        val at = position()
        translate(at.x.coerceIn(0f, size.width) - radius, at.y.coerceIn(0f, size.height) - radius) {
            drawLayer(pool)
        }
    }

    private fun DrawScope.reclip(shape: Shape) {
        clipSize = size
        clipDirection = layoutDirection
        val outline = shape.createOutline(size, layoutDirection, this)
        rectangular = outline is Outline.Rectangle
        if (!rectangular) {
            clip.rewind()
            clip.addOutline(outline)
        }
    }

    /** A pool fully lit to half its reach, then fading out: the catalog's smoothstep, in three stops. */
    private fun DrawScope.record(pool: GraphicsLayer, radius: Float) {
        poolRadius = radius
        val side = ceil(radius * 2f).toInt()
        pool.record(IntSize(side, side)) {
            drawCircle(
                brush = Brush.radialGradient(
                    0f to Color.White,
                    0.5f to Color.White,
                    0.75f to Color.White.copy(alpha = 0.5f),
                    1f to Color.Transparent,
                    center = Offset(radius, radius),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(radius, radius),
            )
        }
    }
}

private const val SheenAlpha = 0.08f
private const val PoolAlpha = 0.15f

/** The pool's radius, as a share of the glass's shorter side. */
private const val PoolReach = 1.5f
