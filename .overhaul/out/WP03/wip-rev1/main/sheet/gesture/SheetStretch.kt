package com.base.app.core.designsystem.component.sheet.gesture

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetStop
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * The rubber band a sheet pulled past an edge follows: iOS's `c · x · d / (d + c · x)`, the same
 * curve overscroll and pull to refresh use, so every edge in the app resists alike. [room] is the
 * asymptote `d`: what is shown approaches it however far the finger goes.
 *
 * Above the tallest stop the room runs to Large's stretch line (B-24), so a sheet at Large gives a
 * few pixels and a Medium-only sheet stretches toward Large; below the lowest stop, while the sheet
 * may not be dismissed, it runs to the hidden offset (B-25).
 */
internal class SheetBand(private val room: Float, private val resistance: Float = DesignTokens.Motion.rubberBand) {

    /** What is shown for [pull] pixels of finger travel past the edge. */
    fun shown(pull: Float): Float {
        if (pull <= 0f || room <= 0f) return 0f
        return resistance * pull * room / (room + resistance * pull)
    }

    /** The finger travel that shows [shown]: the inverse, so a caught band resumes where it is. */
    fun pull(shown: Float): Float {
        if (shown <= 0f || room <= 0f) return 0f
        val clamped = shown.coerceAtMost(room * MaxShare)
        return clamped * room / (resistance * (room - clamped))
    }

    /** How far the sheet moves per pixel of finger travel at [pull]: the release velocity's scale. */
    fun slope(pull: Float): Float {
        if (room <= 0f) return 0f
        val denominator = room + resistance * pull.coerceAtLeast(0f)
        return resistance * room * room / (denominator * denominator)
    }

    companion object {
        /** B-24: the band above a sheet whose tallest stop is at [top]. */
        fun above(top: Float, largeTop: Float, boundsHeight: Float): SheetBand =
            SheetBand(top - (largeTop - DesignTokens.Sheet.Stretch.aboveLarge * boundsHeight))

        /** B-25: the band below a sheet that may not be dismissed, from its lowest stop to hidden. */
        fun below(lowest: Float, hidden: Float): SheetBand = SheetBand(hidden - lowest)

        /** Keeps the inverse finite: a band is never shown quite at its asymptote. */
        private const val MaxShare = 0.999f
    }
}

/**
 * The sheet's drag past its edges, handed to `anchoredDraggable` as its overscroll effect: what the
 * anchors cannot take goes into the rubber band, and a release goes through B-22 instead of
 * foundation's default fling. Nothing is drawn here: the layout places the sheet at
 * [AppSheetState.offset], which includes the band.
 */
internal class SheetStretch(private val state: AppSheetState) : OverscrollEffect {
    override val isInProgress: Boolean
        get() = state.stretch != 0f

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        if (!state.dragging) {
            state.dragging = true
            state.gestureStart = state.currentDetent
        }
        val consumed = state.dragBy(delta.y, bandAbove = true, bandBelow = true) { performScroll(Offset(0f, it)).y }
        return Offset(0f, consumed)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        state.release(velocity.y)
    }
}

/**
 * Moves the sheet by [delta] px of finger travel: first out of any rubber band it is in, then
 * through [moveAnchored] within its anchors, and what is left past an edge into the band there when
 * that edge has one ([bandAbove] past the tallest stop; [bandBelow] past the lowest while the sheet
 * may not be dismissed). Returns how much of [delta] the sheet took.
 */
internal fun AppSheetState.dragBy(
    delta: Float,
    bandAbove: Boolean,
    bandBelow: Boolean,
    moveAnchored: (Float) -> Float = anchored::dispatchRawDelta,
): Float {
    val stops = stops ?: return 0f
    if (delta == 0f || anchored.offset.isNaN()) return 0f
    var remaining = delta
    if (stretch < 0f) {
        val band = SheetBand.above(stops.top, stops.largeTop, stops.hidden)
        val pull = band.pull(-stretch) - remaining
        if (pull > 0f) {
            stretch = -band.shown(pull)
            return delta
        }
        stretch = 0f
        remaining = -pull
    } else if (stretch > 0f) {
        val band = SheetBand.below(stops.lowest, stops.hidden)
        val pull = band.pull(stretch) + remaining
        if (pull > 0f) {
            stretch = band.shown(pull)
            return delta
        }
        stretch = 0f
        remaining = pull
    }
    val left = remaining - moveAnchored(remaining)
    return when {
        left < 0f && bandAbove -> {
            stretch = -SheetBand.above(stops.top, stops.largeTop, stops.hidden).shown(-left)
            delta
        }
        left > 0f && bandBelow && !anchored.anchors.hasPositionFor(SheetStop.Hidden) -> {
            stretch = SheetBand.below(stops.lowest, stops.hidden).shown(left)
            delta
        }
        else -> delta - left
    }
}
