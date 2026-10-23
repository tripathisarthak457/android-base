package com.example.standard.core.designsystem.component.sheet

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.FrequentlyChangingValue
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.example.standard.core.designsystem.component.sheet.gesture.exit
import com.example.standard.core.designsystem.component.sheet.gesture.present
import com.example.standard.core.designsystem.component.sheet.gesture.settleTo
import com.example.standard.core.designsystem.component.sheet.layout.DetentInputs
import com.example.standard.core.designsystem.component.sheet.layout.SheetStops
import kotlinx.coroutines.flow.first

/**
 * Where a sheet rests and how it moves, kept outside composition so a presenter can show it, hide
 * it and read its position from layout and draw.
 *
 * It wraps foundation's AnchoredDraggableState: a drag, a fling, a programmatic change and a window
 * resize go through one mutex, so any of them interrupts another and the motion continues from the
 * current offset at the current velocity.
 *
 * [offset], [presentationFraction], [expansionFraction], [visibleTop] and [visibleHeight] change on
 * every frame of a drag: read them in a layout, layer or draw block only. [currentDetent],
 * [isVisible], [isLeaving] and [isMoving] change at most once per settle and may be read in
 * composition. [targetDetent] also changes while a finger drags across a midpoint.
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

    /** The resolved stops, written in measure only when they change. */
    internal var stops: SheetStops? by mutableStateOf(null)
    internal var inputs: DetentInputs? = null

    /** The attached layout's side of every decision: springs, haptics, the caller's callbacks. */
    internal var host: SheetHost? by mutableStateOf(null)

    /**
     * The latest instance of each custom detent, by id, so a resolver that captured new values is the
     * one measure uses; [resolverGeneration] moves when one is replaced, and measure reads it.
     */
    internal var resolved: List<SheetDetent> = detents
        private set
    internal var resolverGeneration by mutableIntStateOf(0)

    internal var shown by mutableStateOf(false)
    internal var leaving by mutableStateOf(false)
    private var current by mutableStateOf<SheetDetent?>(null)
    private var target by mutableStateOf<SheetDetent?>(null)

    /** A finger or a scroll gesture is moving the sheet. */
    internal var dragging by mutableStateOf(false)

    /** A show, hide, settle or programmatic move is running. */
    internal var animating by mutableStateOf(false)
    internal var presenting by mutableStateOf(false)

    /** A pointer is down on the sheet: programmatic moves wait for it to lift. */
    internal var pointerDown = false

    /** The detent a gesture started from, so landing elsewhere is a change the person made. */
    internal var gestureStart: SheetDetent? = null

    /** The rubber band past an edge, in pixels: negative past the tallest stop, positive past the lowest. */
    internal var stretch by mutableFloatStateOf(0f)

    /** 0 in place to 1 slid away, while the owner is covered. Placement reads it. */
    internal var parkProgress by mutableFloatStateOf(0f)

    /** Whether the owner's tab is the selected one; reads snapshot state, so readers follow a switch. */
    internal var ownerTabSelected: () -> Boolean = { true }

    /**
     * True while the owner's tab is not the selected one: parked on the switch frame, unanimated. A
     * sheet that is leaving finishes its exit drawn.
     */
    internal val parkedInstantly: Boolean
        get() = !leaving && !ownerTabSelected()

    /** A predictive back gesture's eased progress, 0 to 1, read in the sheet's layer blocks. */
    internal var backProgress by mutableFloatStateOf(0f)

    /** How far a persistent sheet follows a back gesture down before stepping down, in pixels. */
    internal var backFollow by mutableFloatStateOf(0f)

    /** The sheet layer's alpha during a cross-fade: 1 except while it fades in or out. */
    internal var fade by mutableFloatStateOf(1f)

    /** How far the keyboard has lifted the sheet this frame, written by placement while it moves. */
    internal var lift by mutableFloatStateOf(0f)

    /** The bounds' top in window pixels, written by placement when it changes. */
    internal var boundsTop by mutableFloatStateOf(0f)

    /** How far the resting sheet rides the minimising tab bar this frame, in pixels. */
    internal var barRide by mutableFloatStateOf(0f)

    /** The sheet blocks what is behind it: written by placement, read by the overlay host and focus. */
    internal var blocking by mutableStateOf(false)

    /** A detent to arrive at without sliding: a restored sheet, or a preview. */
    internal var restoredDetent: SheetDetent? = null

    /** The detent the sheet rests on or is settling onto; null while hidden or leaving. */
    val currentDetent: SheetDetent?
        get() = if (shown && !leaving) current else null

    /**
     * Where the sheet is heading: while it settles, the detent it settles onto; while a finger drags
     * it, the detent a release without velocity would reach. Null while hidden or leaving.
     */
    val targetDetent: SheetDetent?
        get() = if (shown && !leaving) target else null

    /** True from show() until the sheet has left after hide() or a dismissal. */
    val isVisible: Boolean
        get() = shown

    /** True from the moment the sheet is decided to close, by the person or by hide(), until it has gone. */
    val isLeaving: Boolean
        get() = leaving

    /** True while the sheet is dragged or animates. */
    val isMoving: Boolean
        get() = dragging || animating

    /** The sheet's top edge in px from the top of its bounds, before the stretch, lift and park; NaN before its first layout. */
    @get:FrequentlyChangingValue
    val offset: Float
        get() = anchored.offset

    /** 0 hidden, 1 at the lowest active detent and above. The dim follows it. */
    @get:FrequentlyChangingValue
    val presentationFraction: Float
        get() {
            val stops = stops ?: return 0f
            val y = anchored.offset
            if (y.isNaN()) return 0f
            val travel = stops.hidden - stops.lowest
            return if (travel <= 0f) 1f else ((stops.hidden - y) / travel).coerceIn(0f, 1f)
        }

    /**
     * 0 at the highest partial detent and below, 1 at [SheetDetent.Large] or a stop as tall; 0 for a
     * form sheet and a side sheet. For a sheet whose only stop is Large it rises with the present.
     * The card stack, the floating-to-attached change and the glass opacity follow it.
     */
    @get:FrequentlyChangingValue
    val expansionFraction: Float
        get() {
            val stops = stops ?: return 0f
            if (stops.single || !bottomPlaced || stops.largeIndex < 0) return 0f
            val y = anchored.offset
            if (y.isNaN()) return 0f
            val large = stops.offsets[stops.largeIndex]
            val partial = if (stops.largeIndex + 1 < stops.count) stops.offsets[stops.largeIndex + 1] else stops.hidden
            return if (partial <= large) 1f else ((partial - y) / (partial - large)).coerceIn(0f, 1f)
        }

    /** Written in measure: false for a form sheet or a side panel, which never expand. */
    internal var bottomPlaced by mutableStateOf(true)

    /**
     * The top of the sheet's visible surface in window px, after the rubber band, the keyboard lift,
     * parking and its owner's movement; NaN while hidden. For UI that rides the sheet, such as a
     * button kept above it: read it in `Modifier.offset { }` or another placement block.
     */
    @get:FrequentlyChangingValue
    val visibleTop: Float
        get() = if (!shown) Float.NaN else boundsTop + placedTop()

    /** Window px from [visibleTop] to the window's bottom edge, 0 while hidden: the padding that keeps a map's controls clear. */
    @get:FrequentlyChangingValue
    val visibleHeight: Float
        get() {
            if (!shown) return 0f
            val stops = stops ?: return 0f
            return (stops.hidden - placedTop()).coerceAtLeast(0f)
        }

    /** Where the sheet is placed in its bounds this frame: the one formula placement and [visibleTop] share. */
    internal fun placedTop(): Float {
        val stops = stops ?: return Float.NaN
        val y = anchored.offset
        if (y.isNaN()) return stops.hidden
        val parked = parkProgress * (stops.hidden - y)
        return y + stretch + backFollow - lift + parked + barRide
    }

    /**
     * Shows the sheet at [detent], or its initial detent. A sheet that is leaving turns around. For
     * a sheet shown through `AppSheet(visible)`, the caller's `visible` decides: while it is false
     * this returns at once.
     */
    suspend fun show(detent: SheetDetent? = null) {
        if (host?.callerHides == true) return
        if (shown && !leaving) {
            if (detent != null) animateTo(detent)
            return
        }
        val turning = leaving
        shown = true
        leaving = false
        snapshotFlow { host?.readyToPresent == true && stops != null }.first { it }
        present(detent, turning)
    }

    /**
     * Slides the sheet away and returns once it has gone. A second call joins the first. For a
     * sheet shown through `AppSheet(visible)` or `AppSheet(item)`, closing it this way while the
     * caller still shows it calls onDismissRequest once, so the caller's value follows.
     */
    suspend fun hide() {
        if (!shown) return
        if (leaving) {
            snapshotFlow { !shown || !leaving }.first { it }
            return
        }
        host?.onHiddenByCode()
        exit(velocity = anchored.lastVelocity)
    }

    /**
     * Moves to [detent], keeping the current velocity. Returns without moving while a finger is on
     * the sheet, while it leaves or is parked. On a form or side sheet it records [detent] as the
     * detent to take back at the bottom edge and does not move.
     */
    suspend fun animateTo(detent: SheetDetent) {
        val stops = stops ?: return
        if (stops.single) {
            if (detent in detents) record(detent)
            return
        }
        if (!canMoveProgrammatically()) return
        settleTo(activeFor(detent), velocity = anchored.lastVelocity, byPerson = false)
    }

    /** Puts the sheet at [detent] at once: for restoring a sheet, and for tests. */
    suspend fun snapTo(detent: SheetDetent) {
        val stops = stops ?: return
        val stop = if (stops.single) stops.detents[0] else activeFor(detent)
        shown = true
        leaving = false
        fade = 1f
        stretch = 0f
        anchored.anchoredDrag(SheetStop.At(stop), MutatePriority.PreventUserInput) { anchors, latest ->
            dragTo(anchors.positionOf(latest))
        }
        arrive(stop)
    }

    /** One detent up; false at the top, and always on a form or side sheet. */
    suspend fun expand(): Boolean {
        val stops = stops ?: return false
        val index = stops.indexOf(currentDetent ?: return false)
        if (stops.single || index <= 0 || !canMoveProgrammatically()) return false
        settleTo(stops.detents[index - 1], anchored.lastVelocity, byPerson = false)
        return true
    }

    /** One detent down, never to hidden; false at the bottom, and always on a form or side sheet. */
    suspend fun collapse(): Boolean {
        val stops = stops ?: return false
        val index = stops.indexOf(currentDetent ?: return false)
        if (stops.single || index < 0 || index == stops.count - 1 || !canMoveProgrammatically()) return false
        settleTo(stops.detents[index + 1], anchored.lastVelocity, byPerson = false)
        return true
    }

    /**
     * Replaces the detents. A sheet resting on one that stays active stays; otherwise it animates to
     * the nearest active one.
     */
    fun updateDetents(detents: List<SheetDetent>) {
        require(detents.isNotEmpty()) { "A sheet needs at least one detent" }
        refreshResolvers(detents)
        if (detents == this.detents) return
        this.detents = detents
        if (initialDetent !in detents) initialDetent = detents.first()
    }

    /** Takes the latest resolver of every custom detent; measure re-resolves when one changed. */
    internal fun refreshResolvers(latest: List<SheetDetent>) {
        var changed = latest.size != resolved.size
        if (!changed) {
            for (index in latest.indices) {
                if (latest[index] !== resolved[index]) changed = true
            }
        }
        if (!changed) return
        val replacedResolver = latest.any { it is SheetDetent.Custom && resolved.none { old -> old === it } }
        resolved = latest
        if (replacedResolver) resolverGeneration++
    }

    /** Settled on [detent]: the arrival frame of a settle, a snap, or a present. */
    internal fun arrive(detent: SheetDetent) {
        current = detent
        target = detent
    }

    internal fun heading(detent: SheetDetent?) {
        if (target != detent) target = detent
    }

    internal fun record(detent: SheetDetent) {
        current = detent
        target = detent
    }

    /** The decision to close is made: in this frame, before anything animates. */
    internal fun beginLeaving() {
        leaving = true
        target = null
    }

    internal fun finishHidden() {
        shown = false
        leaving = false
        current = null
        target = null
        stretch = 0f
        backProgress = 0f
        backFollow = 0f
        fade = 1f
    }

    internal fun canMoveProgrammatically(): Boolean =
        shown && !leaving && !pointerDown && parkProgress == 0f && !parkedInstantly

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
                AppSheetState(detents, initial).also { if (wasShown) it.restoredDetent = detent ?: initial }
            },
        )
    }
}

/**
 * An [AppSheetState] kept across recomposition, configuration changes and process death. A sheet
 * restored this way appears at its detent without sliding in. A changed [detents] list, or a
 * [SheetDetent.Custom] whose resolver captured new values, is applied on the next layout.
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
