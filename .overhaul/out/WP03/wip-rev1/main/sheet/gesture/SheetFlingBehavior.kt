package com.base.app.core.designsystem.component.sheet.gesture

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetDismissSource
import com.base.app.core.designsystem.component.sheet.SheetStop
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The person let go of the sheet at [velocity] (px/s, positive downward): B-22 picks where it goes.
 * A dismissal runs `onDismissRequest` in this frame and the sheet leaves with the release's speed;
 * a release in the rubber band springs back out of it; anything else settles on a stop.
 */
internal suspend fun AppSheetState.release(velocity: Float) {
    dragging = false
    val start = gestureStart
    gestureStart = null
    if (leaving) return
    val stops = stops ?: return
    val environment = environment ?: return
    val decision = releaseTarget(
        offset = offset,
        velocity = velocity,
        offsets = stops.offsets,
        hidden = stops.hidden,
        dismissAllowed = behaviour.dismissAllowed,
        thresholds = ReleaseThresholds.of(environment.density.density, stops.hidden),
    )
    if (decision.attempted) behaviour.attempted(SheetDismissSource.Drag)
    when (val target = decision.target) {
        SheetRelease.Dismiss -> {
            behaviour.dismissed(SheetDismissSource.Drag)
            leave(velocity)
        }
        is SheetRelease.Stop -> if (stretch != 0f) {
            relaxStretch(velocity)
        } else {
            settleTo(stops.detents[target.index], velocity, byPerson = true, from = start)
        }
    }
}

/**
 * Settles on [detent] on the sheet spring, seeded with [velocity] and, after a retarget, with the
 * velocity it had. The arrival haptic plays once, on the first frame within a pixel of a detent
 * other than [from], and only for the person's own moves; [byPerson] also reports the change.
 */
internal suspend fun AppSheetState.settleTo(
    detent: SheetDetent,
    velocity: Float,
    byPerson: Boolean,
    haptic: Boolean = byPerson,
    from: SheetDetent? = currentDetent,
) {
    val environment = environment ?: return
    val spring = environment.springs.sheet.pixels
    var seed = velocity
    var played = false
    anchored.anchoredDrag(SheetStop.At(detent), MutatePriority.Default) { anchors, latest ->
        val target = anchors.positionOf(latest)
        val arriving = (latest as? SheetStop.At)?.detent
        animate(anchored.offset, target, seed, spring) { value, speed ->
            dragTo(value, speed)
            if (!played && haptic && arriving != from && abs(value - target) < 1f) {
                played = true
                environment.perform(AppHaptic.Threshold)
            }
        }
        seed = anchored.lastVelocity
    }
    if (byPerson && detent != from) behaviour.detentChanged(detent)
}

/** Out of the rubber band, back to the edge it was pulled past, moving as the finger moved it. */
internal suspend fun AppSheetState.relaxStretch(velocity: Float) {
    val environment = environment ?: return
    val stops = stops ?: return
    val band = if (stretch < 0f) SheetBand.above(stops.top, stops.largeTop, stops.hidden) else SheetBand.below(stops.lowest, stops.hidden)
    val moving = velocity * band.slope(band.pull(abs(stretch)))
    anchored.anchoredDrag(MutatePriority.Default) {
        animate(stretch, 0f, moving, environment.springs.sheet.pixels) { value, _ -> stretch = value }
    }
}

/**
 * Moves a resting sheet to where [detent] now is, on the sheet spring: its content changed height,
 * or its detent left the list. The new anchors go in while the mutex is held, so they cannot snap
 * the sheet there; the settle restarts with them and animates. A detent the old anchors do not
 * know has nowhere to animate from and snaps.
 */
internal fun CoroutineScope.launchSettle(state: AppSheetState, detent: SheetDetent) {
    val target = SheetStop.At(detent)
    if (!state.anchored.anchors.hasPositionFor(target)) {
        state.syncAnchors(target)
        return
    }
    launch {
        val spring = state.environment?.springs?.sheet?.pixels ?: return@launch
        state.anchored.anchoredDrag(target, MutatePriority.Default) { anchors, latest ->
            state.syncAnchors(latest)
            animate(state.anchored.offset, anchors.positionOf(latest), state.anchored.lastVelocity, spring) { value, speed ->
                dragTo(value, speed)
            }
        }
    }
}
