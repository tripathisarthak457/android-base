package com.base.app.core.designsystem.component.sheet.gesture

import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/** Where a released sheet goes: one of its stops, by index (0 is the tallest), or away. */
internal sealed interface SheetRelease {
    data class Stop(val index: Int) : SheetRelease

    data object Dismiss : SheetRelease
}

/**
 * The speeds and distances the release rule weighs, in pixels and pixels per second. The defaults
 * are the sheet tokens; tests pass their own.
 */
internal class ReleaseThresholds(
    /** F: a flick at least this fast moves exactly one stop. */
    val detentFlick: Float,
    /** D: a downward flick at least this fast from the lowest stop dismisses. */
    val dismissFlick: Float,
    /** T: how far ahead a release is projected, iOS's 0.99-per-millisecond deceleration. */
    val projectionSeconds: Float = DesignTokens.Sheet.Release.projectionSeconds,
    /** Past this share of the lowest-stop-to-hidden travel, a projected release dismisses. */
    val dismissPosition: Float = DesignTokens.Sheet.Release.dismissPosition,
) {
    companion object {
        fun of(density: Float, boundsHeight: Float) = ReleaseThresholds(
            detentFlick = DesignTokens.Sheet.Release.detentFlick.value * density,
            dismissFlick = DesignTokens.Sheet.Release.dismissFlick * boundsHeight,
        )
    }
}

/**
 * B-22: where a sheet released at [offset] with [velocity] (positive downward) goes, given its stops
 * ([offsets], tallest first) and its hidden offset. A flick moves one stop and never past the
 * lowest; dismissal takes a hard downward flick or a projection past [ReleaseThresholds.dismissPosition];
 * anything else lands on the stop nearest the projected position. [attempted] is true when the
 * release would have dismissed a sheet that may not be dismissed.
 */
internal fun releaseTarget(
    offset: Float,
    velocity: Float,
    offsets: FloatArray,
    hidden: Float,
    dismissAllowed: Boolean,
    thresholds: ReleaseThresholds,
): ReleaseDecision {
    val top = offsets.first()
    val lowest = offsets.last()
    val last = offsets.lastIndex
    if (offset < top) return ReleaseDecision(SheetRelease.Stop(0))
    if (offset <= lowest) {
        val flick = abs(velocity) >= thresholds.detentFlick
        if (flick && velocity > 0f) {
            val below = offsets.indexOfFirst { it > offset }
            if (below >= 0) return ReleaseDecision(SheetRelease.Stop(below))
            return belowLowest(offset, velocity, lowest, last, hidden, dismissAllowed, thresholds)
        }
        if (flick) {
            val above = offsets.indexOfLast { it < offset }
            return ReleaseDecision(SheetRelease.Stop(if (above >= 0) above else 0))
        }
        val projected = (offset + velocity * thresholds.projectionSeconds).coerceIn(top, lowest)
        return ReleaseDecision(SheetRelease.Stop(nearestIndex(offsets, projected)))
    }
    return belowLowest(offset, velocity, lowest, last, hidden, dismissAllowed, thresholds)
}

/** What [releaseTarget] decided, and whether a blocked dismissal was attempted on the way. */
internal data class ReleaseDecision(val target: SheetRelease, val attempted: Boolean = false)

private fun belowLowest(
    offset: Float,
    velocity: Float,
    lowest: Float,
    last: Int,
    hidden: Float,
    dismissAllowed: Boolean,
    thresholds: ReleaseThresholds,
): ReleaseDecision {
    if (-velocity >= thresholds.detentFlick) return ReleaseDecision(SheetRelease.Stop(last))
    val projected = offset + velocity * thresholds.projectionSeconds
    val travel = (hidden - lowest).coerceAtLeast(1f)
    val wouldDismiss = velocity >= thresholds.dismissFlick || (projected - lowest) / travel > thresholds.dismissPosition
    return when {
        !wouldDismiss -> ReleaseDecision(SheetRelease.Stop(last))
        dismissAllowed -> ReleaseDecision(SheetRelease.Dismiss)
        else -> ReleaseDecision(SheetRelease.Stop(last), attempted = true)
    }
}

private fun nearestIndex(offsets: FloatArray, position: Float): Int {
    var best = 0
    for (index in 1..offsets.lastIndex) {
        if (abs(offsets[index] - position) < abs(offsets[best] - position)) best = index
    }
    return best
}
