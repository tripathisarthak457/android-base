package com.base.app.core.designsystem.foundation.glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawModifierNode
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.shadow.DropShadowPainter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo

/**
 * Glass drawn without sampling anything behind it: a translucent [GlassColors.body], light pooling
 * near the top-left corner, a rim bright along the top and faint at the bottom, and a [shadow]
 * that falls only outside the shape.
 *
 * Use it for every piece of glass inside a screen (cards, chips, fields, buttons, sheets) and for
 * chrome in the frosted tier: a live backdrop under each of them would cost a blur pass per frame,
 * where this costs a few draws of brushes built once per size. The shadow is Compose's cached drop
 * shadow with the inside cut away, because a see-through body would otherwise show it as a dark
 * band. Content is not clipped to [shape].
 *
 * Do not fade an ancestor of it with a layer alpha: an offscreen layer is clipped to its bounds and
 * cuts the shadow off square. Fade with `CompositingStrategy.ModulateAlpha`, which needs no buffer.
 */
fun Modifier.glassSurface(
    shape: Shape,
    colors: GlassColors,
    shadow: Shadow? = null,
): Modifier = this then GlassSurfaceElement(shape, colors, shadow, drawsBody = true)

/**
 * The rim and the outer shadow of [glassSurface] alone, for chrome whose body is the blurred scene
 * it samples: the backdrop is drawn as this modifier's content, between the shadow and the rim.
 */
internal fun Modifier.glassEdges(
    shape: Shape,
    colors: GlassColors,
    shadow: Shadow?,
): Modifier = this then GlassSurfaceElement(shape, colors, shadow, drawsBody = false)

private data class GlassSurfaceElement(
    val shape: Shape,
    val colors: GlassColors,
    val shadow: Shadow?,
    val drawsBody: Boolean,
) : ModifierNodeElement<GlassSurfaceNode>() {
    override fun create() = GlassSurfaceNode(shape, colors, shadow, drawsBody)

    override fun update(node: GlassSurfaceNode) = node.update(shape, colors, shadow, drawsBody)

    override fun InspectorInfo.inspectableProperties() {
        name = if (drawsBody) "glassSurface" else "glassEdges"
        properties["shape"] = shape
        properties["colors"] = colors
        properties["shadow"] = shadow
    }
}

private class GlassSurfaceNode(
    private var shape: Shape,
    private var colors: GlassColors,
    private var shadow: Shadow?,
    private var drawsBody: Boolean,
) : DelegatingNode() {

    private var shadowPainter: DropShadowPainter? = null
    private val cache = delegate(CacheDrawModifierNode { buildDrawing() })

    fun update(shape: Shape, colors: GlassColors, shadow: Shadow?, drawsBody: Boolean) {
        if (shape == this.shape && colors == this.colors && shadow == this.shadow &&
            drawsBody == this.drawsBody
        ) {
            return
        }
        if (shape != this.shape || shadow != this.shadow) shadowPainter = null
        this.shape = shape
        this.colors = colors
        this.shadow = shadow
        this.drawsBody = drawsBody
        cache.invalidateDrawCache()
    }

    private fun CacheDrawScope.buildDrawing(): DrawResult {
        val outline = shape.createOutline(size, layoutDirection, this)
        val rimWidth = GlassDefaults.RimWidth.toPx()
        // The rim is stroked on the shape inset by half its width, so it stays inside the edge.
        val rimSize = Size(
            (size.width - rimWidth).coerceAtLeast(0f),
            (size.height - rimWidth).coerceAtLeast(0f),
        )
        val rimOutline = shape.createOutline(rimSize, layoutDirection, this)
        val rim = Brush.verticalGradient(
            0f to colors.rim,
            1f to colors.rim.copy(alpha = colors.rim.alpha * RimFade),
        )
        val rimStroke = Stroke(rimWidth)
        val sheen = if (drawsBody && colors.sheen.alpha > 0f) {
            Brush.radialGradient(
                0f to colors.sheen,
                1f to Color.Transparent,
                center = Offset(size.width * SheenX, 0f),
                radius = size.maxDimension * SheenReach,
            )
        } else {
            null
        }
        val painter = shadow?.let { obtainShadowPainter(it) }
        val inside = if (painter != null) Path().apply { addOutline(outline) } else null
        return onDrawWithContent {
            if (painter != null && inside != null) {
                clipPath(inside, ClipOp.Difference) { with(painter) { draw(size) } }
            }
            if (drawsBody) {
                drawOutline(outline, colors.body)
                if (sheen != null) drawOutline(outline, sheen)
            }
            drawContent()
            translate(rimWidth / 2f, rimWidth / 2f) {
                drawOutline(rimOutline, rim, style = rimStroke)
            }
        }
    }

    // Shared through the window's shadow context, which rasterises each size and shape once.
    private fun obtainShadowPainter(shadow: Shadow): DropShadowPainter =
        shadowPainter
            ?: requireGraphicsContext().shadowContext.createDropShadowPainter(shape, shadow)
                .also { shadowPainter = it }
}

/** How much of the rim's strength is left at the bottom edge. */
private const val RimFade = 0.25f

/**
 * Where the sheen pools, as a share of the width from the left, and how far it reaches. Light falls
 * from the same side in either layout direction.
 */
private const val SheenX = 0.3f
private const val SheenReach = 0.8f
