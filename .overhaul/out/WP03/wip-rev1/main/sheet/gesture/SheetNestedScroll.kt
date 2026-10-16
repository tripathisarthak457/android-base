package com.base.app.core.designsystem.component.sheet.gesture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.base.app.core.designsystem.component.sheet.AppSheetState
import kotlin.math.abs

/**
 * How scrolling inside a sheet moves the sheet (B-27 to B-32). With [resizes] true (iOS's default
 * for a modal sheet on touch) a scroll up grows the sheet to its tallest stop before the content
 * moves; with it false the content scrolls first at every stop. Either way, pulling down at the
 * content's top moves the sheet down, toward hidden or into the band when it may not close.
 *
 * Only the finger moves the sheet: a fling's momentum and an accessibility scroll are not
 * `UserInput`, so they bounce in the content's own edge and never resize the sheet.
 *
 * It also measures how far the content has scrolled under the header ([scrolled], in px), which
 * the header's scroll-edge fade reads in draw.
 */
internal class SheetNestedScroll(private val state: AppSheetState, private val resizes: () -> Boolean) : NestedScrollConnection {

    /** How far the content has moved up under the header since it was last at its top, in px. */
    var scrolled by mutableFloatStateOf(0f)
        private set

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (source != NestedScrollSource.UserInput || available.y >= 0f || !movable()) return Offset.Zero
        // A band below the lowest stop gives way before anything else does.
        val growing = resizes() && state.anchored.offset > (state.stops?.top ?: return Offset.Zero) + Epsilon
        if (state.stretch <= 0f && !growing) return Offset.Zero
        begin()
        return Offset(0f, state.dragBy(available.y, bandAbove = false, bandBelow = false))
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (consumed.y < 0f) scrolled += -consumed.y
        if (consumed.y > 0f) scrolled = (scrolled - consumed.y).coerceAtLeast(0f)
        if (available.y > 0f) scrolled = 0f
        if (source != NestedScrollSource.UserInput || available.y <= 0f || !movable()) return Offset.Zero
        begin()
        return Offset(0f, state.dragBy(available.y, bandAbove = false, bandBelow = true))
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (state.gestureStart == null && !state.dragging) return Velocity.Zero
        if (resting()) {
            state.dragging = false
            state.gestureStart = null
            return Velocity.Zero
        }
        state.release(available.y)
        return available
    }

    private fun begin() {
        if (state.gestureStart == null) state.gestureStart = state.currentDetent
        state.dragging = true
    }

    private fun movable(): Boolean = state.isVisible && !state.leaving && state.parkProgress == 0f

    private fun resting(): Boolean {
        val stops = state.stops ?: return true
        if (state.stretch != 0f) return false
        val offset = state.anchored.offset
        return stops.offsets.any { abs(it - offset) < Epsilon }
    }

    private companion object {
        const val Epsilon = 0.5f
    }
}
