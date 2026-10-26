package com.example.finalafter.crui2.core.designsystem.component.sheet.gesture

import com.example.finalafter.crui2.core.designsystem.component.sheet.layout.RestPx
import com.example.finalafter.crui2.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/** The speeds and the share a release is judged by, in pixels and pixels per second. */
internal class ReleaseThresholds(
    /** F: a flick at least this fast moves exactly one stop. */
    val detentFlick: Float,
    /** D: a flick toward hidden at least this fast from the lowest stop dismisses. */
    val dismissFlick: Float,
    /** T: how far ahead a release is projected, iOS's 0.99-per-millisecond deceleration. */
    val projectionSeconds: Float = DesignTokens.Sheet.Release.projectionSeconds,
    /** The share of the lowest-stop-to-hidden travel, at the release, past which the sheet closes. */
    val dismissPosition: Float = DesignTokens.Sheet.Release.dismissPosition,
) {
    companion object {
        /** The tokens for a sheet whose travel to hidden spans [travelLength] pixels (H, or W for a side panel). */
        fun of(density: Float, travelLength: Float) = ReleaseThresholds(
            detentFlick = DesignTokens.Sheet.Release.detentFlick.value * density,
            dismissFlick = DesignTokens.Sheet.Release.dismissFlick * travelLength,
        )
    }
}

/** Where a released sheet goes: a stop by index (0 is the tallest), or away. */
internal sealed interface SheetRelease {
    data class Stop(val index: Int) : SheetRelease

    data object Dismiss : SheetRelease
}

/**
 * What a release decided, and whether it would have closed a sheet the person may not close: then
 * the guard hears of the attempt, once for the gesture.
 */
internal data class ReleaseDecision(val target: SheetRelease, val attempted: Boolean = false)

/**
 * Where a sheet let go at [offset] with [velocity] goes. Both grow toward hidden; [offsets]
 * are the stops, tallest first. A deliberate flick moves exactly one stop, never past the lowest;
 * closing takes a flick of two bound heights a second, or a release most of the way out; anything
 * slower lands on the stop nearest where the release would coast to.
 */
internal fun releaseTarget(
    offset: Float,
    velocity: Float,
    offsets: FloatArray,
    hidden: Float,
    dismissAllowed: Boolean,
    thresholds: ReleaseThresholds,
): ReleaseDecision {
    val top = offsets[0]
    val lowest = offsets[offsets.size - 1]
    if (offset < top) return ReleaseDecision(SheetRelease.Stop(0))
    if (offset > lowest) return belowLowest(offset, velocity, offsets, hidden, dismissAllowed, thresholds)
    if (velocity >= thresholds.detentFlick) {
        for (index in offsets.indices) {
            if (offsets[index] > offset + RestPx) return ReleaseDecision(SheetRelease.Stop(index))
        }
        return belowLowest(offset, velocity, offsets, hidden, dismissAllowed, thresholds)
    }
    if (-velocity >= thresholds.detentFlick) {
        for (index in offsets.indices.reversed()) {
            if (offsets[index] < offset - RestPx) return ReleaseDecision(SheetRelease.Stop(index))
        }
        return ReleaseDecision(SheetRelease.Stop(0))
    }
    val projected = (offset + velocity * thresholds.projectionSeconds).coerceIn(top, lowest)
    return ReleaseDecision(SheetRelease.Stop(nearest(offsets, projected)))
}

private fun belowLowest(
    offset: Float,
    velocity: Float,
    offsets: FloatArray,
    hidden: Float,
    dismissAllowed: Boolean,
    thresholds: ReleaseThresholds,
): ReleaseDecision {
    val last = offsets.size - 1
    if (-velocity >= thresholds.detentFlick) return ReleaseDecision(SheetRelease.Stop(last))
    val lowest = offsets[last]
    val travel = (hidden - lowest).coerceAtLeast(1f)
    // Judged where the sheet was let go, not where it would coast: a slow drag most of the way out
    // closes it, a quick small flick that has not gone far does not.
    val wouldClose = velocity >= thresholds.dismissFlick || (offset - lowest) / travel >= thresholds.dismissPosition
    return when {
        !wouldClose -> ReleaseDecision(SheetRelease.Stop(last))
        dismissAllowed -> ReleaseDecision(SheetRelease.Dismiss)
        else -> ReleaseDecision(SheetRelease.Stop(last), attempted = true)
    }
}

private fun nearest(offsets: FloatArray, position: Float): Int {
    var best = 0
    for (index in 1 until offsets.size) {
        if (abs(offsets[index] - position) < abs(offsets[best] - position)) best = index
    }
    return best
}
