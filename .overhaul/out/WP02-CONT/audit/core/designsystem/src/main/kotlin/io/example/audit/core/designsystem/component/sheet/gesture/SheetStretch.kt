package io.example.audit.core.designsystem.component.sheet.gesture

import androidx.compose.foundation.OverscrollEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import io.example.audit.core.designsystem.component.sheet.AppSheetState
import io.example.audit.core.designsystem.component.sheet.SheetStop
import io.example.audit.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/**
 * The rubber band a sheet pulled past an edge follows: iOS's `c · x · d / (d + c · x)`, the curve
 * overscroll and pull to refresh use too, so every edge in the app resists alike. [room] is the
 * asymptote `d`: what shows approaches it however far the finger goes.
 */
internal class SheetBand(private val room: Float, private val resistance: Float = DesignTokens.Motion.rubberBand) {

    /** What shows for [pull] pixels of finger travel past the edge. */
    fun shown(pull: Float): Float {
        if (pull <= 0f || room <= 0f) return 0f
        return resistance * pull * room / (room + resistance * pull)
    }

    /** The finger travel that shows [shown]: the inverse, so a band caught again resumes where it is. */
    fun pull(shown: Float): Float {
        if (shown <= 0f || room <= 0f) return 0f
        val clamped = shown.coerceAtMost(room * MaxShare)
        return clamped * room / (resistance * (room - clamped))
    }

    /** How far the sheet moves per pixel of finger travel at [pull]: what scales a release's velocity. */
    fun slope(pull: Float): Float {
        if (room <= 0f) return 0f
        val denominator = room + resistance * pull.coerceAtLeast(0f)
        return resistance * room * room / (denominator * denominator)
    }

    private companion object {
        /** Keeps the inverse finite: a band never shows quite its asymptote. */
        const val MaxShare = 0.999f
    }
}

/**
 * The sheet's own drag, handed to `anchoredDraggable` as its overscroll effect: what the anchors
 * cannot take goes into the rubber band, and a release goes through the sheet's own release rule rather than
 * foundation's default fling. Nothing draws here; placement reads the band through [AppSheetState.stretch].
 */
internal class SheetStretch(
    private val state: AppSheetState,
    private val horizontal: Boolean,
    private val onDragDelta: (Float) -> Unit,
) : OverscrollEffect {
    override val isInProgress: Boolean
        get() = state.stretch != 0f

    override fun applyToScroll(delta: Offset, source: NestedScrollSource, performScroll: (Offset) -> Offset): Offset {
        val along = if (horizontal) delta.x else delta.y
        if (!state.dragging) {
            state.dragging = true
            state.gestureStart = state.currentDetent
        }
        onDragDelta(along)
        val consumed = state.dragBy(along) { amount -> performScroll(axis(amount)).let { if (horizontal) it.x else it.y } }
        return axis(consumed)
    }

    override suspend fun applyToFling(velocity: Velocity, performFling: suspend (Velocity) -> Velocity) {
        state.release(if (horizontal) velocity.x else velocity.y)
    }

    private fun axis(amount: Float): Offset = if (horizontal) Offset(amount, 0f) else Offset(0f, amount)
}

/**
 * Moves the sheet by [delta] pixels of finger travel: first out of any rubber band it is in, then
 * through [moveAnchored] within its anchors, and what is left past an edge into the band there. Past
 * the lowest stop there is a band only while Hidden is not an anchor, which is how a sheet the person
 * may not close resists instead; past the tallest only with [bandAbove], which a list's scroll leaves
 * out so the list scrolls the rest. Returns how much of [delta] the sheet took.
 */
internal fun AppSheetState.dragBy(delta: Float, bandAbove: Boolean = true, moveAnchored: (Float) -> Float): Float {
    val stops = stops ?: return 0f
    if (delta == 0f || anchored.offset.isNaN()) return 0f
    var remaining = delta
    if (stretch != 0f) {
        val band = bandFor(stretch)
        val pull = band.pull(abs(stretch)) + if (stretch < 0f) -remaining else remaining
        if (pull > 0f) {
            stretch = if (stretch < 0f) -band.shown(pull) else band.shown(pull)
            trackTarget()
            return delta
        }
        remaining = if (stretch < 0f) -pull else pull
        stretch = 0f
    }
    val left = remaining - moveAnchored(remaining)
    when {
        left < 0f && bandAbove -> stretch = -SheetBand(stops.roomAbove).shown(-left)
        left < 0f -> {
            trackTarget()
            return delta - left
        }
        left > 0f && !anchored.anchors.hasPositionFor(SheetStop.Hidden) ->
            stretch = SheetBand(stops.hidden - stops.lowest).shown(left)
        left > 0f -> {
            trackTarget()
            return delta - left
        }
    }
    trackTarget()
    return delta
}

/** While a finger drags, [AppSheetState.targetDetent] is where a still release would land. */
private fun AppSheetState.trackTarget() {
    val stops = stops ?: return
    val decision = releaseTarget(
        offset = anchored.offset + stretch,
        velocity = 0f,
        offsets = stops.offsets,
        hidden = stops.hidden,
        dismissAllowed = true,
        thresholds = NoFlick,
    )
    val target = decision.target
    heading(if (target is SheetRelease.Stop) stops.detents[target.index] else null)
}

private val NoFlick = ReleaseThresholds(detentFlick = Float.MAX_VALUE, dismissFlick = Float.MAX_VALUE)
