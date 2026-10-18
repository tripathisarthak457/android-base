package com.example.wp04.everything.core.designsystem.foundation.interaction

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * Reserves at least the 48dp touch target around content that draws smaller, and centres it there.
 * The reserved space belongs to the node, so a pointer input before this modifier covers all of it.
 */
fun Modifier.minimumTouchTarget(): Modifier = this then MinimumTouchTargetElement

private object MinimumTouchTargetElement : ModifierNodeElement<MinimumTouchTargetNode>() {
    override fun create() = MinimumTouchTargetNode()

    override fun update(node: MinimumTouchTargetNode) = Unit

    override fun InspectorInfo.inspectableProperties() {
        name = "minimumTouchTarget"
    }

    override fun equals(other: Any?) = other === this

    override fun hashCode() = 2
}

private class MinimumTouchTargetNode : Modifier.Node(), LayoutModifierNode {
    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        val target = MinTouchTarget.roundToPx()
        val width = constraints.constrainWidth(maxOf(placeable.width, target))
        val height = constraints.constrainHeight(maxOf(placeable.height, target))
        return layout(width, height) {
            placeable.place((width - placeable.width) / 2, (height - placeable.height) / 2)
        }
    }
}

internal val MinTouchTarget = 48.dp
