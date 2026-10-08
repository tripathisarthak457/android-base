package com.base.app.core.navigation

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * The depth cue of a push: while a screen slides over another, it darkens the strip beside its
 * leading edge, which is exactly the part of the covered screen still in view, and casts a soft
 * shadow along that edge. [covered] runs from 0 (this screen off screen) to 1 (fully in place).
 *
 * One rectangle and one gradient per frame in the draw phase, with no layer: fading the covered
 * screen instead would render all of it offscreen on every frame. Nothing is drawn once the screen
 * has settled.
 */
internal fun Modifier.coverScrim(covered: State<Float>, maxAlpha: Float, shadow: Dp): Modifier =
    this then CoverScrimElement(covered, maxAlpha, shadow)

/** How dark the covered screen gets: lighter on a light theme, where black reads heavier. */
internal fun coverScrimAlpha(isLight: Boolean): Float = if (isLight) 0.10f else 0.24f

private data class CoverScrimElement(
    val covered: State<Float>,
    val maxAlpha: Float,
    val shadow: Dp,
) : ModifierNodeElement<CoverScrimNode>() {
    override fun create() = CoverScrimNode(covered, maxAlpha, shadow)

    override fun update(node: CoverScrimNode) {
        node.covered = covered
        node.maxAlpha = maxAlpha
        node.shadow = shadow
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "coverScrim"
    }
}

private class CoverScrimNode(
    var covered: State<Float>,
    var maxAlpha: Float,
    var shadow: Dp,
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        val fraction = covered.value
        if (fraction > 0f && fraction < 1f) {
            val ltr = layoutDirection == LayoutDirection.Ltr
            drawRect(
                color = Color.Black,
                topLeft = Offset(if (ltr) -size.width else size.width, 0f),
                size = size,
                alpha = maxAlpha * fraction,
            )
            val edge = shadow.toPx()
            val dark = Color.Black.copy(alpha = SHADOW_ALPHA * fraction)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = if (ltr) listOf(Color.Transparent, dark) else listOf(dark, Color.Transparent),
                    startX = if (ltr) -edge else size.width,
                    endX = if (ltr) 0f else size.width + edge,
                ),
                topLeft = Offset(if (ltr) -edge else size.width, 0f),
                size = Size(edge, size.height),
            )
        }
        drawContent()
    }

    private companion object {
        const val SHADOW_ALPHA = 0.12f
    }
}
