package com.base.app.core.designsystem.component.overlay

import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * Hides the app beneath a modal overlay from screen readers, and keeps focus traversal out of it,
 * for as long as one is up. The flag is read from a flow and applied by invalidating semantics, so
 * a non-modal sheet crossing into blocking recomposes nothing.
 */
internal fun Modifier.hiddenBeneathOverlays(state: OverlayHostState): Modifier = this then HiddenBeneathElement(state)

private data class HiddenBeneathElement(val state: OverlayHostState) : ModifierNodeElement<HiddenBeneathNode>() {
    override fun create() = HiddenBeneathNode(state)

    override fun update(node: HiddenBeneathNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "hiddenBeneathOverlays"
    }
}

private class HiddenBeneathNode(var state: OverlayHostState) : Modifier.Node(), SemanticsModifierNode {
    private var hidden = false

    override val shouldClearDescendantSemantics: Boolean
        get() = hidden

    override fun onAttach() {
        coroutineScope.launch {
            snapshotFlow { state.modal }.collect { modal ->
                state.trapsFocus = modal
                if (modal != hidden) {
                    hidden = modal
                    invalidateSemantics()
                }
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        if (hidden) hideFromAccessibility()
    }
}

/**
 * An overlay whose caller's tab is not the selected one stays laid out, so its layers and
 * display lists survive the switch, but is moved off the window and drawn at alpha 0, which the
 * renderer skips, and is not announced. It returns in place on the switch frame.
 */
internal fun Modifier.parkedOverlay(parked: () -> Boolean): Modifier = this then ParkedOverlayElement(parked)

private class ParkedOverlayElement(val parked: () -> Boolean) : ModifierNodeElement<ParkedOverlayNode>() {
    override fun create() = ParkedOverlayNode(parked)

    override fun update(node: ParkedOverlayNode) {
        node.parked = parked
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "parkedOverlay"
    }

    override fun equals(other: Any?): Boolean = other is ParkedOverlayElement && other.parked === parked

    override fun hashCode(): Int = parked.hashCode()
}

private class ParkedOverlayNode(var parked: () -> Boolean) : Modifier.Node(), LayoutModifierNode, SemanticsModifierNode {
    private var hidden = false

    override val shouldClearDescendantSemantics: Boolean
        get() = hidden

    override fun onAttach() {
        coroutineScope.launch {
            snapshotFlow { parked() }.collect { now ->
                if (now != hidden) {
                    hidden = now
                    invalidateSemantics()
                }
            }
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() = Unit

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val away = parked()
            placeable.placeWithLayer(0, if (away) placeable.height * 2 else 0) { alpha = if (away) 0f else 1f }
        }
    }
}
