package io.example.fitcnetdb.core.designsystem.component.sheet.gesture

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.PointerInputModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.IntSize
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetController
import io.example.fitcnetdb.core.designsystem.component.sheet.SheetDetent
import kotlin.math.abs
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * A finger landing anywhere on a moving sheet, content included, stops it where it is, as a
 * finger stops a scrolling list. It runs on the Initial pass, before the content sees the down, and
 * consumes nothing then, so the content's own gestures still work; but if the down stopped the sheet
 * and the finger lifts without dragging, the lift is consumed, so stopping a sheet does not also tap
 * the row under the finger, and the sheet finishes the move it was making.
 *
 * It also records what pointer each gesture is: a wheel or a touchpad scroll never
 * resizes a sheet.
 */
internal fun Modifier.sheetCatch(controller: SheetController): Modifier = this then SheetCatchElement(controller)

private data class SheetCatchElement(val controller: SheetController) : ModifierNodeElement<SheetCatchNode>() {
    override fun create() = SheetCatchNode(controller)

    override fun update(node: SheetCatchNode) {
        node.controller = controller
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "sheetCatch"
    }
}

private class SheetCatchNode(var controller: SheetController) :
    Modifier.Node(), PointerInputModifierNode, CompositionLocalConsumerModifierNode {

    private var heading: SheetDetent? = null
    private var stopped = false
    private var down = Offset.Zero

    override fun onPointerEvent(pointerEvent: PointerEvent, pass: PointerEventPass, bounds: IntSize) {
        if (pass != PointerEventPass.Initial) return
        val change = pointerEvent.changes.firstOrNull() ?: return
        when (pointerEvent.type) {
            PointerEventType.Scroll -> controller.gesturePointer = PointerType.Mouse
            PointerEventType.Press -> if (pointerEvent.changes.count { it.pressed } == 1) onDown(change.position, change.type)
            PointerEventType.Move -> if (stopped) {
                val slop = currentValueOf(LocalViewConfiguration).touchSlop
                val moved = change.position - down
                // A drag takes the sheet from here; a sideways scroll takes the pointer, and the sheet
                // goes on to where it was heading.
                if (abs(moved.y) > slop) {
                    stopped = false
                } else if (abs(moved.x) > slop) {
                    stopped = false
                    resume()
                }
            }
            PointerEventType.Release -> if (pointerEvent.changes.none { it.pressed }) onUp(pointerEvent)
        }
    }

    private fun onDown(position: Offset, type: PointerType) {
        val state = controller.sheetState
        state.pointerDown = true
        controller.gesturePointer = type
        down = position
        stopped = false
        if (state.isLeaving || state.parkProgress > 0f || state.parkedInstantly) return
        if (!state.animating || state.dragging) return
        heading = state.targetDetent ?: state.initialDetent
        stopped = true
        // Undispatched, so the running motion is cancelled before the drag that may follow asks for
        // the mutex.
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) { state.stopMotion() }
    }

    private fun onUp(event: PointerEvent) {
        controller.sheetState.pointerDown = false
        if (!stopped) return
        stopped = false
        event.changes.forEach { it.consume() }
        resume()
    }

    /** No haptic and no change reported: the sheet only finishes the move the person interrupted. */
    private fun resume() {
        val state = controller.sheetState
        val target = heading ?: return
        heading = null
        if (state.isLeaving || state.dragging) return
        coroutineScope.launch { state.settleTo(target, velocity = 0f, byPerson = false) }
    }

    override fun onCancelPointerInput() {
        controller.sheetState.pointerDown = false
        if (stopped) {
            stopped = false
            resume()
        }
    }
}
