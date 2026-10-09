package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.base.app.core.designsystem.theme.AppSpring
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlin.math.abs

/** Where a sheet can rest: a little over half the window, or nearly all of it. */
enum class SheetDetent { Medium, Large }

/**
 * A sheet's position, kept outside composition so whoever presents it can also take it away
 * animated: Back through the navigation stack, a programmatic pop, a parent hiding it.
 *
 * [dismiss] is idempotent: while a dismissal runs, another Back or tap does nothing, so the sheet
 * goes once and never takes the screen beneath with it.
 */
@Stable
class AppSheetState(
    val detents: Set<SheetDetent> = setOf(SheetDetent.Medium, SheetDetent.Large),
    private val initialDetent: SheetDetent = if (SheetDetent.Medium in detents) SheetDetent.Medium else SheetDetent.Large,
) {
    init {
        require(detents.isNotEmpty()) { "A sheet needs at least one detent" }
        require(initialDetent in detents) { "$initialDetent is not one of $detents" }
    }

    /** The sheet's top edge, from the top of the area it is shown in. NaN until it is laid out. */
    internal var offset by mutableFloatStateOf(Float.NaN)
    internal var anchors by mutableStateOf<SheetAnchors?>(null)
    internal var spring: AppSpring? = null

    /** True from the moment the sheet starts to leave. */
    var dismissing by mutableStateOf(false)
        private set

    private val motion = MutatorMutex()
    private var shown = false

    /** Where the current drag began, so a settle can tell whether it changed detent. */
    private var dragStart = Float.NaN

    /** The detent the sheet rests nearest, or null while it is hidden. */
    val currentDetent: SheetDetent?
        get() {
            val a = anchors ?: return null
            if (offset.isNaN() || dismissing) return null
            return detents.minByOrNull { abs(a.offsetOf(it) - offset) }
        }

    /** How far the sheet is shown, 0 hidden to 1 at its first detent; drives the scrim. */
    internal val progress: Float
        get() {
            val a = anchors ?: return 0f
            if (offset.isNaN()) return 0f
            val rest = a.offsetOf(detents.maxBy { a.offsetOf(it) })
            return ((a.hidden - offset) / (a.hidden - rest)).coerceIn(0f, 1f)
        }

    suspend fun animateTo(detent: SheetDetent) {
        require(detent in detents) { "$detent is not one of $detents" }
        val a = laidOut()
        animateOffset(a.offsetOf(detent))
    }

    /** Slides the sheet away and then calls [onGone], once, however many times this is called. */
    suspend fun dismiss(onGone: () -> Unit = {}) {
        if (dismissing) return
        dismissing = true
        anchors?.let { animateOffset(it.hidden) }
        onGone()
    }

    internal suspend fun show(animate: Boolean) {
        if (shown) return
        shown = true
        val a = laidOut()
        val target = a.offsetOf(initialDetent)
        if (animate) {
            offset = a.hidden
            animateOffset(target)
        } else {
            offset = target
        }
    }

    private suspend fun laidOut(): SheetAnchors = snapshotFlow { anchors }.filterNotNull().first()

    internal suspend fun stop() {
        motion.mutate(MutatePriority.UserInput) {}
    }

    /** Follows a drag; past the large detent it resists, and it never goes past hidden. */
    internal fun dragBy(delta: Float): Float {
        val a = anchors ?: return 0f
        if (offset.isNaN() || dismissing) return 0f
        if (dragStart.isNaN()) dragStart = offset
        val before = offset
        val top = a.offsetOf(detents.minBy { a.offsetOf(it) })
        val resisted = if (delta < 0f && offset < top) delta * (1f - ((top - offset) / RUBBER_BAND_LIMIT).coerceIn(0f, 1f)) else delta
        offset = (offset + resisted).coerceIn(top - RUBBER_BAND_LIMIT, a.hidden)
        return offset - before
    }

    internal val canExpand: Boolean
        get() {
            val a = anchors ?: return false
            return offset > a.offsetOf(detents.minBy { a.offsetOf(it) }) + 0.5f
        }

    internal val resting: Boolean
        get() {
            val a = anchors ?: return true
            return detents.any { abs(a.offsetOf(it) - offset) < 0.5f }
        }

    /**
     * Settles after a drag, on a detent or off the bottom. [onSnap] runs when it lands on a detent
     * other than the one it started from; the result is true when it went off the bottom.
     */
    internal suspend fun settle(velocity: Float, onSnap: () -> Unit = {}): Boolean {
        val a = anchors ?: return false
        val from = if (dragStart.isNaN()) null else detents.minBy { abs(a.offsetOf(it) - dragStart) }
        dragStart = Float.NaN
        val stops = detents.map { a.offsetOf(it) }.sorted() + a.hidden
        val target = when {
            velocity > FLING_VELOCITY -> stops.firstOrNull { it > offset + 0.5f } ?: a.hidden
            velocity < -FLING_VELOCITY -> stops.lastOrNull { it < offset - 0.5f } ?: stops.first()
            else -> stops.minBy { abs(it - offset) }
        }
        if (target == a.hidden) {
            if (dismissing) return false
            dismissing = true
        }
        animateOffset(target, velocity)
        val landed = detents.firstOrNull { a.offsetOf(it) == target }
        if (landed != null && landed != from) onSnap()
        return target == a.hidden
    }

    internal fun predictiveTrack(start: Float, progress: Float) {
        val a = anchors ?: return
        offset = start + (a.hidden - start) * progress
    }

    internal suspend fun returnTo(target: Float) = animateOffset(target)

    private suspend fun animateOffset(target: Float, velocity: Float = 0f) {
        val spec = spring?.pixels
        motion.mutate {
            if (spec == null || offset.isNaN()) {
                offset = target
            } else {
                animate(offset, target, velocity, spec) { value, _ -> offset = value }
            }
        }
    }

    private companion object {
        const val FLING_VELOCITY = 1400f
        const val RUBBER_BAND_LIMIT = 120f
    }
}

internal class SheetAnchors(val large: Float, val medium: Float, val hidden: Float) {
    fun offsetOf(detent: SheetDetent) = if (detent == SheetDetent.Large) large else medium
}
