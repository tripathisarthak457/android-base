package com.example.finalafter.crui.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui.core.designsystem.theme.LocalAppColors

/** Which edge a [hairline] runs along. */
enum class HairlineEdge { Top, Bottom }

/**
 * A separator one physical pixel thick, as iOS draws them, along [edge], starting [startInset] in
 * from the leading edge (where a row's text begins). Drawn on the element itself, so a list of rows
 * gains no layout nodes for its dividers.
 */
fun Modifier.hairline(edge: HairlineEdge = HairlineEdge.Bottom, startInset: Dp = 0.dp): Modifier =
    this then HairlineElement(edge, startInset)

private data class HairlineElement(val edge: HairlineEdge, val startInset: Dp) : ModifierNodeElement<HairlineNode>() {
    override fun create() = HairlineNode(edge, startInset)

    override fun update(node: HairlineNode) {
        node.edge = edge
        node.startInset = startInset
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "hairline"
        properties["edge"] = edge
        properties["startInset"] = startInset
    }
}

private class HairlineNode(
    var edge: HairlineEdge,
    var startInset: Dp,
) : Modifier.Node(), DrawModifierNode, CompositionLocalConsumerModifierNode {
    override fun ContentDrawScope.draw() {
        drawContent()
        val inset = startInset.toPx()
        val top = if (edge == HairlineEdge.Top) 0f else size.height - 1f
        val (start, end) = if (layoutDirection == LayoutDirection.Ltr) inset to size.width else 0f to size.width - inset
        drawHairline(currentValueOf(LocalAppColors).separator, start, end, top)
    }
}

/**
 * Fills the one-pixel row whose top edge is [top], from [start] to [end]. Every separator one
 * physical pixel thick is drawn through here, as a filled rect rather than a one-pixel `drawLine`
 * over the same row: the GPU renderer draws a stroked line at far greater cost than a fill of the
 * same pixels, and a separator is drawn again in every frame its screen moves or scrolls, the
 * frames where a slow GPU has the least time to spare.
 */
internal fun DrawScope.drawHairline(color: Color, start: Float, end: Float, top: Float, alpha: Float = 1f) {
    if (end > start) drawRect(color, topLeft = Offset(start, top), size = Size(end - start, 1f), alpha = alpha)
}
