package com.example.videoalone.core.designsystem.foundation.draw

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.videoalone.core.designsystem.theme.LocalAppColors

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
        val y = if (edge == HairlineEdge.Top) 0.5f else size.height - 0.5f
        val (start, end) = if (layoutDirection == LayoutDirection.Ltr) inset to size.width else 0f to size.width - inset
        drawLine(currentValueOf(LocalAppColors).separator, Offset(start, y), Offset(end, y), strokeWidth = 1f)
    }
}
