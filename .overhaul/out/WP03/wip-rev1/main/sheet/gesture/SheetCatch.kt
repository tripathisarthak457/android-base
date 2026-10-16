package com.base.app.core.designsystem.component.sheet.gesture

import androidx.compose.foundation.MutatePriority
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
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetStop
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * B-17: a finger landing anywhere on a moving sheet, content included, stops it where it is, as a
 * finger stops a scrolling list. It runs on the Initial pass, before the content sees the down, and
 * consumes nothing, so the content's own gestures still work; but if that down stopped the sheet and
 * the finger lifts without moving, the lift is consumed, so stopping a sheet does not also tap the
 * row under the finger. A sheet stopped that way and let go settles where it was heading.
 */
internal fun Modifier.sheetCatch(state: AppSheetState): Modifier = this then SheetCatchElement(state)

private data class SheetCatchElement(val state: AppSheetState) : ModifierNodeElement<SheetCatchNode>() {
    override fun create() = SheetCatchNode(state)

    override fun update(node: SheetCatchNode) {
        node.state = state
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetCatch"
    }
}

private class SheetCatchNode(var state: AppSheetState) :
    Modifier.Node(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {

    private var caught: SheetDetent? = null
    private var stopped = false
    private var down = Offset.Zero

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        when (pointerEvent.type) {
            PointerEventType.Press -> if (pointerEvent.changes.count { it.pressed } == 1) onDown(change.position)
            PointerEventType.Move -> if (stopped) {
                val slop = currentValueOf(LocalViewConfiguration).touchSlop
                if ((change.position - down).getDistance() > slop) stopped = false
            }
            PointerEventType.Release -> if (pointerEvent.changes.none { it.pressed }) onUp(pointerEvent)
        }
    }

    private fun onDown(position: Offset) {
        val state = state
        state.dragging = true
        down = position
        stopped = false
        if (state.leaving || state.parkProgress > 0f || !state.anchored.isAnimationRunning) return
        caught = (state.anchored.targetValue as? SheetStop.At)?.detent
        stopped = true
        // Undispatched, so the running animation is cancelled before the drag that may follow asks
        // for the mutex at a lower priority.
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            state.anchored.anchoredDrag(MutatePriority.PreventUserInput) {}
        }
    }

    private fun onUp(event: PointerEvent) {
        val state = state
        val heading = caught
        caught = null
        if (stopped) event.changes.forEach { it.consume() }
        stopped = false
        // A drag ends through its release; a touch that never dragged leaves the sheet where it
        // was stopped, so it finishes the move it was making.
        if (state.gestureStart == null && state.dragging) {
            state.dragging = false
            if (heading != null && !state.leaving) {
                coroutineScope.launch { state.settleTo(heading, velocity = 0f, byPerson = false, haptic = false) }
            }
        }
    }

    override fun onCancelPointerInput() {
        caught = null
        stopped = false
        if (state.gestureStart == null) state.dragging = false
    }
}
