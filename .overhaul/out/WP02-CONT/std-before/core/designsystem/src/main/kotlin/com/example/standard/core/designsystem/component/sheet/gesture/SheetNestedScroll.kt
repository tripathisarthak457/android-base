package com.example.standard.core.designsystem.component.sheet.gesture

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDragScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.unit.Velocity
import com.example.standard.core.designsystem.component.sheet.SheetContentInteraction
import com.example.standard.core.designsystem.component.sheet.SheetController
import com.example.standard.core.designsystem.component.sheet.SheetModality
import com.example.standard.core.designsystem.component.sheet.layout.RestPx
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * How scrolling inside a sheet moves the sheet. While the content resizes the sheet
 * (iOS's default for a modal sheet under a finger), a scroll up grows the sheet to its tallest stop
 * before the content moves; while it scrolls, the content scrolls first at every stop. Either way a
 * pull down at the content's top moves the sheet, toward hidden or into the band when it may not
 * close, through the same `dragBy` as the sheet's own drag, so both resist alike.
 *
 * The deltas go through one anchored-drag session per gesture, held from its first delta to its
 * fling, so a running show, hide or park is never fought. Only a finger moves the sheet: a fling's
 * momentum, a wheel, a touchpad and an accessibility scroll are not a finger's user input.
 *
 * It also measures how far the content has scrolled under the header ([scrolled], in px), which the
 * header's scroll-edge fade reads in draw.
 */
internal class SheetNestedScroll(private val controller: SheetController) : NestedScrollConnection {

    var scrolled by mutableFloatStateOf(0f)
        private set

    private var session: AnchoredDragScope? = null
    private var closing: CompletableDeferred<Unit>? = null

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0f || !movable()) return Offset.Zero
        val state = controller.sheetState
        val stops = state.stops ?: return Offset.Zero
        val growing = resizes() && state.anchored.offset > stops.top + RestPx
        if (state.stretch <= 0f && !growing) return Offset.Zero
        val scope = session() ?: return Offset.Zero
        return Offset(0f, state.dragBy(available.y, bandAbove = false) { amount -> scope.moveBy(amount) })
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (consumed.y < 0f) scrolled -= consumed.y
        if (consumed.y > 0f) scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
        if (available.y > 0f) scrolled = 0f
        if (source != NestedScrollSource.UserInput || available.y <= 0f || !movable()) return Offset.Zero
        val scope = session() ?: return Offset.Zero
        val state = controller.sheetState
        return Offset(0f, state.dragBy(available.y) { amount -> scope.moveBy(amount) })
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (closing == null) return Velocity.Zero
        close()
        val state = controller.sheetState
        val stops = state.stops ?: return Velocity.Zero
        if (state.stretch == 0f && stops.isAtStop(state.anchored.offset)) {
            state.dragging = false
            state.gestureStart = null
            return Velocity.Zero
        }
        state.release(available.y)
        return available
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        close()
        return Velocity.Zero
    }

    private fun session(): AnchoredDragScope? {
        session?.let { return it }
        if (closing != null) return null
        val state = controller.sheetState
        val done = CompletableDeferred<Unit>()
        closing = done
        controller.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            state.anchored.anchoredDrag(MutatePriority.Default) {
                session = this
                if (!state.dragging) {
                    state.dragging = true
                    state.gestureStart = state.currentDetent
                }
                try {
                    done.await()
                } finally {
                    session = null
                }
            }
        }
        return session
    }

    private fun close() {
        closing?.complete(Unit)
        closing = null
    }

    private fun AnchoredDragScope.moveBy(amount: Float): Float {
        val anchored = controller.sheetState.anchored
        val from = anchored.offset
        val to = (from + amount).coerceIn(anchored.anchors.minPosition(), anchored.anchors.maxPosition())
        dragTo(to)
        return to - from
    }

    private fun movable(): Boolean {
        val state = controller.sheetState
        val pointer = controller.gesturePointer
        return controller.dragEnabled && !state.presenting && (pointer == PointerType.Touch || pointer == PointerType.Stylus)
    }

    /** A modal sheet under a finger grows first, unless TalkBack explores by touch. */
    private fun resizes(): Boolean = when (controller.properties.contentInteraction) {
        SheetContentInteraction.Resizes -> true
        SheetContentInteraction.Scrolls -> false
        else -> controller.modality is SheetModality.Modal && !controller.touchExploration
    }
}
