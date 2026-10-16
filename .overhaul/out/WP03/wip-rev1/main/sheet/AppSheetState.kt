package com.base.app.core.designsystem.component.sheet

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.component.sheet.gesture.launchSettle
import com.base.app.core.designsystem.component.sheet.gesture.settleTo
import com.base.app.core.designsystem.component.sheet.layout.DetentInputs
import com.base.app.core.designsystem.component.sheet.layout.SheetStops
import com.base.app.core.designsystem.component.sheet.layout.offsetOf
import kotlinx.coroutines.flow.first

/** Where a sheet can be: hidden, or resting at one of its detents. */
internal sealed interface SheetStop {
    data object Hidden : SheetStop

    data class At(val detent: SheetDetent) : SheetStop
}

/**
 * Where a sheet rests and how it moves, kept outside composition so a presenter can show it, hide
 * it and read its position from layout and draw.
 *
 * It wraps foundation's AnchoredDraggableState: a drag, a settle, a programmatic change and a
 * window resize go through one mutex, so any of them interrupts another and the motion continues
 * from the current offset at the current velocity.
 *
 * [offset], [presentationFraction] and [expansionFraction] change on every frame of a drag: read
 * them in a layout, layer or draw block only. [currentDetent], [targetDetent], [isVisible] and
 * [isMoving] change at most once per settle and may be read in composition.
 */
@Stable
class AppSheetState @RememberInComposition constructor(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
) {
    init {
        require(detents.isNotEmpty()) { "A sheet needs at least one detent" }
        require(initialDetent in detents) { "$initialDetent is not one of $detents" }
    }

    /** The heights the sheet can rest at, in any order; at least one. */
    var detents: List<SheetDetent> by mutableStateOf(detents)
        private set

    internal var initialDetent: SheetDetent = initialDetent
        private set

    internal val anchored = AnchoredDraggableState<SheetStop>(SheetStop.Hidden)

    /** The resolved stops and what they were resolved from, written in measure when they change. */
    internal var stops: SheetStops? by mutableStateOf(null)
        private set
    private var inputs: DetentInputs? = null

    /** Lent by the layout node while it is attached. */
    internal var environment: SheetEnvironment? by mutableStateOf(null)

    /** The composable's side of every decision about closing. */
    internal var behaviour: SheetBehaviour = SheetBehaviour.None

    /** Presets arrive on the quicker dialog spring (M17); every other sheet on the sheet spring. */
    internal var arrivesAsDialog = false

    private var shown by mutableStateOf(false)
    internal var leaving by mutableStateOf(false)
        private set

    /** True while a finger is down on the sheet; a drag in progress counts. */
    internal var dragging by mutableStateOf(false)

    /** True while content-side slots registered in the first composition are still to be measured (B-2). */
    internal var awaitingSlots by mutableStateOf(false)

    /** The rubber band past an edge, in pixels: negative above the tallest stop, positive below the lowest. */
    internal var stretch by mutableFloatStateOf(0f)

    /** 1, or 0 to 1 while the sheet fades in or out in place (reduce motion, cross-fade). */
    internal var fade by mutableFloatStateOf(1f)

    /** A predictive back gesture's eased progress, 0 to 1, read in the sheet's layer blocks. */
    internal var backProgress by mutableFloatStateOf(0f)

    /** How far a parked sheet has slid away, 0 in place to 1 hidden, read in placement. */
    internal var parkProgress by mutableFloatStateOf(0f)

    /** The detent a gesture started from, so a settle elsewhere is a change. */
    internal var gestureStart: SheetDetent? = null

    /** A restored detent, applied with the first stops, when the detents can be resolved. */
    private var restoredDetent: SheetDetent? = null

    /** The detent the sheet rests on or is settling onto; null while hidden or leaving. */
    val currentDetent: SheetDetent? by derivedStateOf {
        when {
            !shown || leaving -> null
            anchored.isAnimationRunning -> (anchored.targetValue as? SheetStop.At)?.detent
            else -> (anchored.settledValue as? SheetStop.At)?.detent
        }
    }

    /** Where the sheet is heading: the release target while settling, the nearest stop while dragged. */
    val targetDetent: SheetDetent? by derivedStateOf {
        if (!shown || leaving) null else (anchored.targetValue as? SheetStop.At)?.detent
    }

    /** True from show() until the sheet has left after hide() or a dismissal. */
    val isVisible: Boolean
        get() = shown

    /** True while the sheet is dragged or animates. */
    val isMoving: Boolean by derivedStateOf { anchored.isAnimationRunning || dragging || leaving }

    /** The sheet's top edge in px from the top of its bounds; NaN before its first layout. */
    @get:FrequentlyChangingValue
    val offset: Float
        get() = anchored.offset + stretch

    /** 0 hidden, 1 at the lowest active detent and above. The dim follows it. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float
        get() {
            val stops = stops ?: return 0f
            val y = offset
            if (y.isNaN()) return 0f
            val travel = stops.hidden - stops.lowest
            val slid = if (travel <= 0f) 1f else ((stops.hidden - y) / travel).coerceIn(0f, 1f)
            return slid * fade
        }

    /**
     * 0 at the highest partial detent and below, 1 at [SheetDetent.Large]; 0 for a sheet without
     * Large, a form sheet and a side sheet. For a sheet whose only detent is Large it rises with the
     * present. The card stack, the floating-to-attached change and the glass opacity follow it.
     */
    @get:FrequentlyChangingValue
    val expansionFraction: Float
        get() {
            val stops = stops ?: return 0f
            if (stops.isForm || !bottomPlaced) return 0f
            val large = stops.offsetOf(SheetDetent.Large)
            val y = offset
            if (large.isNaN() || y.isNaN()) return 0f
            val partial = stops.offsets.firstOrNull { it > large } ?: stops.hidden
            return ((partial - y) / (partial - large)).coerceIn(0f, 1f)
        }

    /** Written in measure: false for a side panel, which never expands into a card stack. */
    internal var bottomPlaced = true

    /** Shows the sheet at [detent], or its initial detent. A sheet that is leaving turns around. */
    suspend fun show(detent: SheetDetent? = null) {
        if (shown && !leaving) {
            if (detent != null) animateTo(detent)
            return
        }
        shown = true
        leaving = false
        snapshotFlow { stops != null && environment != null && !awaitingSlots }.first { it }
        val environment = environment ?: return
        val target = SheetStop.At(activeFor(detent ?: initialDetent))
        syncAnchors(target)
        if (environment.crossFade) {
            if (anchored.settledValue == SheetStop.Hidden) fade = 0f
            anchored.anchoredDrag(target, MutatePriority.PreventUserInput) { anchors, latest ->
                dragTo(anchors.positionOf(latest))
                animate(fade, 1f, animationSpec = environment.springs.fadeIn()) { value, _ -> fade = value }
            }
            return
        }
        fade = 1f
        val spring = (if (arrivesAsDialog) environment.springs.dialog else environment.springs.sheet).pixels
        anchored.anchoredDrag(target, MutatePriority.PreventUserInput) { anchors, latest ->
            animate(anchored.offset, anchors.positionOf(latest), anchored.lastVelocity, spring) { value, velocity ->
                dragTo(value, velocity)
            }
        }
    }

    /** Slides the sheet away and returns once it has gone. A second call joins the first. */
    suspend fun hide() {
        leave(velocity = anchored.lastVelocity)
    }

    /** Moves to [detent], keeping the current velocity. Ignored while leaving, parked or under a finger. */
    suspend fun animateTo(detent: SheetDetent) {
        val stops = stops ?: return
        if (!canMoveProgrammatically() || !stops.has(detent)) return
        settleTo(detent, velocity = anchored.lastVelocity, byPerson = false)
    }

    suspend fun snapTo(detent: SheetDetent) {
        val stops = stops ?: return
        if (!stops.has(detent)) return
        shown = true
        leaving = false
        fade = 1f
        anchored.anchoredDrag(SheetStop.At(detent), MutatePriority.PreventUserInput) { anchors, latest ->
            dragTo(anchors.positionOf(latest))
        }
    }

    /** One detent up; false at the top. */
    suspend fun expand(): Boolean {
        val stops = stops ?: return false
        val index = stops.detents.indexOf(currentDetent ?: return false)
        if (index <= 0 || !canMoveProgrammatically()) return false
        settleTo(stops.detents[index - 1], anchored.lastVelocity, byPerson = false)
        return true
    }

    /** One detent down, never to hidden; false at the bottom. */
    suspend fun collapse(): Boolean {
        val stops = stops ?: return false
        val index = stops.detents.indexOf(currentDetent ?: return false)
        if (index < 0 || index == stops.detents.lastIndex || !canMoveProgrammatically()) return false
        settleTo(stops.detents[index + 1], anchored.lastVelocity, byPerson = false)
        return true
    }

    /**
     * Replaces the detents. A sheet resting on one that stays active stays; otherwise it animates to
     * the nearest active one.
     */
    fun updateDetents(detents: List<SheetDetent>) {
        require(detents.isNotEmpty()) { "A sheet needs at least one detent" }
        if (detents == this.detents) return
        this.detents = detents
        if (initialDetent !in detents) initialDetent = detents.first()
    }

    /**
     * New stops from measure. A restored sheet snaps to its detent; a sheet resting on a detent
     * keeps it by identity and snaps to where it now is (a rotation, an unfold), unless [animate]
     * (its content changed height, or its detent left the list), in which case it moves there on
     * the sheet spring.
     */
    internal fun applyStops(new: SheetStops, from: DetentInputs, animate: Boolean) {
        val restored = restoredDetent
        stops = new
        inputs = from
        if (restored != null) {
            restoredDetent = null
            syncAnchors(SheetStop.At(activeFor(restored)))
            return
        }
        val target = anchored.targetValue
        val keeps = target !is SheetStop.At || new.has(target.detent)
        if (!animate && keeps) {
            syncAnchors(target)
            return
        }
        val environment = environment
        if (environment == null || !shown || leaving) {
            syncAnchors()
            return
        }
        val destination = if (target is SheetStop.At) activeFor(target.detent) else return syncAnchors()
        environment.scope.launchSettle(this, destination)
    }

    /** The stop a request for [detent] lands on: the detent, or the active stop nearest it (R-9). */
    internal fun activeFor(detent: SheetDetent): SheetDetent {
        val stops = stops ?: return detent
        if (stops.has(detent)) return detent
        val wouldBe = inputs?.let { offsetOf(detent, it) } ?: return stops.detents.first()
        return stops.nearest(wouldBe)
    }

    /**
     * Writes the anchors for the current stops. Hidden is an anchor while the sheet is hidden,
     * arriving or leaving, and while the person may drag it away; otherwise a drag past the lowest
     * stop runs into the rubber band, which is how dismissal is blocked.
     */
    internal fun syncAnchors(target: SheetStop = anchored.targetValue) {
        val stops = stops ?: return
        val withHidden = !shown || leaving || behaviour.dismissAllowed || anchored.settledValue == SheetStop.Hidden
        val anchors = DraggableAnchors {
            if (withHidden) SheetStop.Hidden at stops.hidden
            stops.detents.forEachIndexed { index, detent -> SheetStop.At(detent) at stops.offsets[index] }
        }
        val kept = if (target is SheetStop.At && !stops.has(target.detent)) SheetStop.At(activeFor(target.detent)) else target
        anchored.updateAnchors(anchors, kept)
    }

    /** Leaves with [velocity]: the release's for a person's dismissal, the current one for code. */
    internal suspend fun leave(velocity: Float) {
        if (!shown) return
        if (leaving) {
            snapshotFlow { !shown || !leaving }.first { it }
            return
        }
        leaving = true
        syncAnchors(SheetStop.Hidden)
        val environment = environment
        anchored.anchoredDrag(SheetStop.Hidden, MutatePriority.PreventUserInput) { anchors, latest ->
            // A sheet let go in its rubber band leaves from where it shows, not from the edge.
            val from = anchored.offset + stretch
            stretch = 0f
            val to = anchors.positionOf(latest)
            when {
                environment == null -> dragTo(to)
                environment.crossFade -> {
                    animate(fade, 0f, animationSpec = environment.springs.fadeOut()) { value, _ -> fade = value }
                    dragTo(to)
                }
                else -> animate(from, to, velocity, environment.springs.sheet.pixels) { value, v -> dragTo(value, v) }
            }
        }
        if (leaving) {
            shown = false
            leaving = false
            backProgress = 0f
            fade = 1f
        }
    }

    private fun canMoveProgrammatically(): Boolean = shown && !leaving && !dragging && parkProgress == 0f

    private fun restore(detent: SheetDetent?) {
        shown = true
        restoredDetent = detent ?: initialDetent
    }

    companion object {
        /**
         * Saves whether the sheet was shown and the identity of the detent it rested on. The detents
         * come back from the caller, because a custom resolver cannot be saved.
         */
        fun Saver(detents: List<SheetDetent>): Saver<AppSheetState, String> = Saver(
            save = { state -> SheetStateKey.encode(state.isVisible, state.currentDetent) },
            restore = { saved ->
                val (wasShown, detent) = SheetStateKey.decode(saved, detents)
                val initial = if (detent != null && detent in detents) detent else detents.first()
                AppSheetState(detents, initial).also { if (wasShown) it.restore(detent) }
            },
        )
    }
}

/**
 * An [AppSheetState] kept across recomposition, configuration changes and process death. A sheet
 * restored this way appears at its detent without sliding in. A changed [detents] list is applied
 * with [AppSheetState.updateDetents].
 */
@Composable
fun rememberAppSheetState(
    detents: List<SheetDetent> = AppSheetDefaults.Detents,
    initialDetent: SheetDetent = detents.first(),
): AppSheetState {
    val state = rememberSaveable(saver = AppSheetState.Saver(detents)) { AppSheetState(detents, initialDetent) }
    SideEffect { state.updateDetents(detents) }
    return state
}

/**
 * The saved form of a sheet: `"<shown>|<key>"`, where the key names the detent it rested on. A
 * custom detent is saved by its id and comes back only if the caller still lists it.
 */
internal object SheetStateKey {
    fun encode(shown: Boolean, detent: SheetDetent?): String = "${if (shown) 1 else 0}|${detent?.let(::key).orEmpty()}"

    fun decode(saved: String, detents: List<SheetDetent>): Pair<Boolean, SheetDetent?> {
        val shown = saved.startsWith("1|")
        val key = saved.substringAfter('|', "")
        val detent = when {
            key == "large" -> SheetDetent.Large
            key == "medium" -> SheetDetent.Medium
            key == "fit" -> SheetDetent.FitContent
            key.startsWith("fraction:") -> key.removePrefix("fraction:").toFloatOrNull()
                ?.takeIf { it > 0f && it <= 1f }?.let { SheetDetent.Fraction(it) }
            key.startsWith("height:") -> key.removePrefix("height:").toFloatOrNull()?.let { SheetDetent.Height(Dp(it)) }
            key.startsWith("custom:") -> key.removePrefix("custom:").let { id -> detents.firstOrNull { it is SheetDetent.Custom && it.id == id } }
            else -> null
        }
        return shown to detent
    }

    private fun key(detent: SheetDetent): String = when (detent) {
        SheetDetent.Large -> "large"
        SheetDetent.Medium -> "medium"
        SheetDetent.FitContent -> "fit"
        is SheetDetent.Fraction -> "fraction:${detent.fraction}"
        is SheetDetent.Height -> "height:${detent.height.value}"
        is SheetDetent.Custom -> "custom:${detent.id}"
    }
}
