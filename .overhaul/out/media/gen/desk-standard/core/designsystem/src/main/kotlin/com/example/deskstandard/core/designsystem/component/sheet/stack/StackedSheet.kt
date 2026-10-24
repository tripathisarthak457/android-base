package com.example.deskstandard.core.designsystem.component.sheet.stack

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.deskstandard.core.designsystem.component.sheet.AppSheetState

/**
 * One sheet as its window's [SheetStack] sees it. The flags are written by the sheet when they
 * change (a settle, a new style, a placement); the getters read the sheet's state, so a presenter
 * that reads the stack in a layer or draw block follows the sheet frame by frame without recomposing.
 */
@Stable
internal class StackedSheet(private val state: AppSheetState) {
    var level = 0

    var modal by mutableStateOf(true)

    /** Presented by the navigation host rather than from a screen: its exit hands Back to the host's gate. */
    var destination = false

    /** Bounded by a pane smaller than the window: it dims its pane itself, never a presenter. */
    var paneBounded by mutableStateOf(false)

    /** Modal, from the bottom of a compact window, with a stop as tall as Large and allowed to recede it. */
    var drivesCardStack by mutableStateOf(false)

    /** As wide as its presenter: at Large nothing of the presenter shows below its top. */
    var spansWidth by mutableStateOf(false)

    /** Edge-attached rather than floating, so at its resting stops it covers the window's bottom edge. */
    var attached by mutableStateOf(false)

    var overlay by mutableStateOf(Color.Black)
    var scrim by mutableStateOf(Color.Transparent)
    var container by mutableStateOf(Color.Transparent)
    var cornerRadius by mutableFloatStateOf(0f)
    var bottomChrome by mutableStateOf(0.dp)

    /** For a non-modal sheet: `upThrough`'s offset and the next stop's above it, between which the dim ramps. */
    var boundary by mutableFloatStateOf(Float.NaN)
    var boundaryNext by mutableFloatStateOf(Float.NaN)

    /** Up and not parked: it counts for levels, the card stack, the dim and Back. */
    fun active(): Boolean = state.isVisible && !state.parkedInstantly && state.parkProgress < 1f

    /** The expansion the presenter recedes by, less however far the sheet has parked. */
    fun expansion(): Float = if (state.parkedInstantly) 0f else state.expansionFraction * (1f - state.parkProgress)

    /** How strongly this sheet's scrim shows: its presentation for a modal sheet, the ramp past `upThrough` for a non-modal one. */
    fun presence(): Float {
        if (state.parkedInstantly) return 0f
        val shown = if (modal) state.presentationFraction * state.fade else ramp()
        return shown * (1f - state.parkProgress)
    }

    /** m: 0 at `upThrough` and below, 1 at the next stop up. */
    fun ramp(): Float {
        val from = boundary
        val to = boundaryNext
        if (from.isNaN()) return 0f
        if (to.isNaN() || to >= from) return 0f
        return ((from - state.offset) / (from - to)).coerceIn(0f, 1f)
    }

    fun blocks(): Boolean = active() && !(destination && state.isLeaving)

    fun coversBottomEdge(): Boolean = active() && attached && state.presentationFraction >= 1f

    fun coversPresenter(): Boolean = spansWidth && state.expansionFraction >= 1f

    fun visibleTop(): Float = state.visibleTop

    /** Where this sheet's tallest stop puts its top, in window pixels; NaN before its first layout. */
    fun topStopInWindow(): Float {
        val stops = state.stops ?: return Float.NaN
        return state.boundsTop + stops.top
    }
}
