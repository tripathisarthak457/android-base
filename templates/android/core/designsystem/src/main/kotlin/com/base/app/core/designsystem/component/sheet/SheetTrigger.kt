package com.base.app.core.designsystem.component.sheet

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Marks a control that opens [state]'s sheet. While a finger is down on it, the sheet composes and
 * measures its content hidden, a slice per frame, so the tap that opens it finds nothing left to build
 * and its first frame is not dropped. A press that becomes a drag lets go at once; one that ends
 * without opening the sheet lets go shortly after. A click without a press, from a keyboard or an
 * accessibility service, opens the sheet as before.
 *
 * Works with `AppSheet(visible = …)` and `AppChoiceSheet(state = …)`, given the state the sheet
 * uses. Put it on the control, not on every row of a list: a press that starts a scroll still pays
 * for the first slice.
 */
fun Modifier.sheetTrigger(state: AppSheetState): Modifier = this then SheetTriggerElement(state)

private class SheetTriggerElement(private val state: AppSheetState) : ModifierNodeElement<SheetTriggerNode>() {
    override fun create() = SheetTriggerNode(state)

    override fun update(node: SheetTriggerNode) = node.update(state)

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetTrigger"
        properties["state"] = state
    }

    override fun equals(other: Any?): Boolean = other is SheetTriggerElement && other.state === state

    override fun hashCode(): Int = System.identityHashCode(state)
}

private class SheetTriggerNode(private var state: AppSheetState) :
    Modifier.Node(),
    PointerInputModifierNode,
    CompositionLocalConsumerModifierNode {

    /** This node prepared the sheet and has not let go yet. */
    private var owns = false
    private var downAt = Offset.Zero
    private var release: Job? = null

    fun update(state: AppSheetState) {
        if (state === this.state) return
        letGo()
        this.state = state
    }

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        when (pass) {
            // Initial: the press is seen before the control's own handling, and nothing is consumed here.
            PointerEventPass.Initial -> if (pointerEvent.type == PointerEventType.Press) {
                val change = pointerEvent.changes.singleOrNull() ?: return
                if (!change.pressed || change.isConsumed) return
                release?.cancel()
                downAt = change.position
                owns = true
                state.prepare()
            }
            // Final: what the control and any scroll around it did with the event is known. The control's
            // own tap detection consumes the press and the lift, so only a consumed move means a scroll took it.
            PointerEventPass.Final -> if (owns) {
                val change = pointerEvent.changes.firstOrNull() ?: return
                when (pointerEvent.type) {
                    PointerEventType.Release -> letGoSoon()
                    PointerEventType.Move -> {
                        val slop = currentValueOf(LocalViewConfiguration).touchSlop
                        if (change.isConsumed || (change.position - downAt).getDistance() > slop) letGo()
                    }
                }
            }
            PointerEventPass.Main -> Unit
        }
    }

    override fun onCancelPointerInput() = letGo()

    override fun onDetach() {
        release = null
        if (owns) {
            owns = false
            state.releasePreparation()
        }
    }

    /** The click that opens the sheet comes with the lift and shows it within two frames; anything later is not this press. */
    private fun letGoSoon() {
        release?.cancel()
        release = coroutineScope.launch {
            delay(RELEASE_AFTER_MILLIS)
            release = null
            letGo()
        }
    }

    private fun letGo() {
        release?.cancel()
        release = null
        if (!owns) return
        owns = false
        state.releasePreparation()
    }

    private companion object {
        const val RELEASE_AFTER_MILLIS = 400L
    }
}
