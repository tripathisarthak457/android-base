package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
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
class AppSheetState @RememberInComposition constructor(
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
    internal var rubberBand = 0f

    /** How far a system back gesture has gone, 0 to 1; the sheet shrinks toward its bottom edge with it. */
    internal var backProgress by mutableFloatStateOf(0f)

    /** True from the moment the sheet starts to leave. */
    var dismissing by mutableStateOf(false)
        private set

    private val motion = MutatorMutex()
    private var shown = false

    /** Where the current drag began, so a settle can tell whether it changed detent. */
    private var dragStart = Float.NaN

    /** The finger's travel past the top detent; what is shown of it is rubber-banded. */
    private var overshoot = 0f

    /** The detent the sheet rests nearest, or null while it is hidden. */
    val currentDetent: SheetDetent?
        get() {
            val a = anchors ?: return null
            if (offset.isNaN() || dismissing) return null
            return detents.minByOrNull { abs(a.offsetOf(it) - offset) }
        }

    /**
     * How far toward its large detent the sheet is: 0 at the medium detent or anywhere below it, 1 at
     * the large detent. A presenter reads it to recede behind a large sheet, as iOS's card stack does,
     * in a layer or draw block: it changes on every frame of a drag.
     */
    @get:FrequentlyChangingValue
    val largeFraction: Float
        get() {
            val a = anchors ?: return 0f
            // A form sheet floats over the screen like an alert; nothing recedes behind it.
            if (offset.isNaN() || a.form || SheetDetent.Large !in detents) return 0f
            val from = if (SheetDetent.Medium in detents && a.medium > a.large) a.medium else a.hidden
            return ((from - offset) / (from - a.large)).coerceIn(0f, 1f)
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

    /**
     * New anchors after the window changed size: a sheet resting on a detent stays on it, so a
     * rotation or an unfold never leaves it floating where the old detent was.
     */
    internal fun updateAnchors(new: SheetAnchors) {
        val old = anchors
        if (old == new) return
        val detent = if (old != null && !offset.isNaN() && !dismissing) detents.firstOrNull { abs(old.offsetOf(it) - offset) < 0.5f } else null
        anchors = new
        if (detent != null) offset = new.offsetOf(detent)
    }

    private suspend fun laidOut(): SheetAnchors = snapshotFlow { anchors }.filterNotNull().first()

    internal suspend fun stop() {
        motion.mutate(MutatePriority.UserInput) {}
    }

    /**
     * Follows a drag. Past the top detent the sheet gives less the further it is pulled, on iOS's
     * rubber band over the window's height, and it never goes past hidden.
     */
    internal fun dragBy(delta: Float): Float {
        val a = anchors ?: return 0f
        if (offset.isNaN() || dismissing) return 0f
        if (dragStart.isNaN()) {
            dragStart = offset
            overshoot = 0f
        }
        val before = offset
        val top = a.offsetOf(detents.minBy { a.offsetOf(it) })
        if (overshoot > 0f || offset + delta < top) {
            val past = overshoot - delta - (offset - top).coerceAtLeast(0f)
            overshoot = past.coerceAtLeast(0f)
            offset = if (past > 0f) top - rubberBand(past, a.hidden) else (top - past).coerceAtMost(a.hidden)
        } else {
            offset = (offset + delta).coerceIn(top, a.hidden)
        }
        return offset - before
    }

    private fun rubberBand(travel: Float, range: Float): Float = travel * rubberBand * range / (range + travel * rubberBand)

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
        overshoot = 0f
        // The stop nearest to where the release would coast, as iOS picks a detent: a flick carries
        // the sheet a stop further, a slow release lands where it was let go.
        val projected = offset + velocity * ProjectionSeconds
        val stops = detents.map { a.offsetOf(it) } + a.hidden
        val target = stops.minBy { abs(it - projected) }
        if (target == a.hidden) {
            if (dismissing) return false
            dismissing = true
        }
        animateOffset(target, velocity)
        val landed = detents.firstOrNull { a.offsetOf(it) == target }
        if (landed != null && landed != from) onSnap()
        return target == a.hidden
    }

    /** A back gesture the user took back: the sheet grows to its size again. */
    internal suspend fun cancelBack() {
        val spec = spring?.unit ?: return run { backProgress = 0f }
        animate(backProgress, 0f, animationSpec = spec) { value, _ -> backProgress = value }
    }

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
        /** iOS's paging deceleration (0.99 per ms) coasts `v · 0.99 / (1 − 0.99) / 1000` px: 0.099 s of `v`. */
        const val ProjectionSeconds = 0.099f
    }
}

/** Where each detent puts the sheet's top edge. A form sheet rests at one place, the centre. */
internal data class SheetAnchors(val large: Float, val medium: Float, val hidden: Float, val form: Boolean) {
    fun offsetOf(detent: SheetDetent) = if (detent == SheetDetent.Large) large else medium
}
