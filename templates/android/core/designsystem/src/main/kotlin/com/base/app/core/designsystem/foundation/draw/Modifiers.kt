package com.base.app.core.designsystem.foundation.draw

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.layout
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.LocalAppColors

/** The single definition of what "disabled" looks like. */
fun Modifier.disabledAlpha(enabled: Boolean): Modifier = if (enabled) this else alpha(DISABLED_ALPHA)

const val DISABLED_ALPHA = 0.38f

/**
 * Lets content run [amount] past its parent's horizontal padding on both sides: a carousel that
 * reaches the screen edges inside a column padded to the gutter.
 */
fun Modifier.bleed(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.placeRelative(-extra / 2, 0)
    }
}

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

/**
 * The shape of one row in a group drawn as a single card from separate items (a lazy list cannot
 * wrap its rows in one background): the first row takes the card's top corners, the last its bottom
 * ones, and the rows between are square.
 */
fun Shape.groupItem(first: Boolean, last: Boolean): Shape {
    if (this !is CornerBasedShape) return this
    val none = CornerSize(0)
    return when {
        first && last -> this
        first -> copy(bottomStart = none, bottomEnd = none)
        last -> copy(topStart = none, topEnd = none)
        else -> RectangleShape
    }
}
