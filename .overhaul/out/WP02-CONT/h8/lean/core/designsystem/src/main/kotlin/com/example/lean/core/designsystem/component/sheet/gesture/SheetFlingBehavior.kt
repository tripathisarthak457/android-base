package com.example.lean.core.designsystem.component.sheet.gesture

import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDragScope
import androidx.compose.foundation.gestures.DraggableAnchors
import com.example.lean.core.designsystem.component.sheet.AppSheetState
import com.example.lean.core.designsystem.component.sheet.SheetDetent
import com.example.lean.core.designsystem.component.sheet.SheetDismissSource
import com.example.lean.core.designsystem.component.sheet.SheetStop
import com.example.lean.core.designsystem.component.sheet.activeFor
import com.example.lean.core.designsystem.component.sheet.syncAnchors
import com.example.lean.core.designsystem.foundation.interaction.AppHaptic
import com.example.lean.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/**
 * The person let go of the sheet at [velocity] (px/s, positive toward hidden): [releaseTarget] picks
 * where it goes. A dismissal runs `onDismissRequest` in this frame and the sheet leaves at the release's
 * speed; a release past an edge comes out of the rubber band at the speed the band let it move; any
 * other release settles on a stop.
 */
internal suspend fun AppSheetState.release(velocity: Float) {
    dragging = false
    val start = gestureStart
    gestureStart = null
    if (leaving || !shown) return
    val stops = stops ?: return
    val host = host ?: return
    val visual = anchored.offset + stretch
    val decision = releaseTarget(
        offset = visual,
        velocity = velocity,
        offsets = stops.offsets,
        hidden = stops.hidden,
        dismissAllowed = host.dismissAllowed,
        thresholds = ReleaseThresholds.of(host.density, stops.span),
    )
    when (val target = decision.target) {
        SheetRelease.Dismiss -> host.closeByPerson(SheetDismissSource.Drag, velocity)
        is SheetRelease.Stop -> {
            if (decision.attempted) host.attempt(SheetDismissSource.Drag)
            val seed = if (stretch == 0f) velocity else velocity * bandFor(stretch).slope(bandFor(stretch).pull(abs(stretch)))
            settleTo(stops.detents[target.index], seed, byPerson = true, from = start)
        }
    }
}

/**
 * Settles on [detent] on the sheet spring, seeded with [velocity]; a retarget keeps the velocity it
 * has. For the person's own moves the arrival haptic plays once, on the first frame with at most 2 %
 * of the travel (or a pixel) left to a detent other than [from], and the change is reported then.
 */
internal suspend fun AppSheetState.settleTo(
    detent: SheetDetent,
    velocity: Float,
    byPerson: Boolean,
    from: SheetDetent? = currentDetent,
    haptic: Boolean = byPerson,
) {
    val host = host ?: return
    heading(detent)
    var seed = velocity
    var arrived = false
    animating = true
    try {
        anchored.anchoredDrag(SheetStop.At(detent), MutatePriority.Default) { anchors, latest ->
            val to = anchors.positionOf(latest)
            val start = anchored.offset + stretch
            val near = maxOf(1f, DesignTokens.Sheet.Haptic.arrivalShare * abs(to - start))
            animate(start, to, seed, host.springs.sheet.pixels) { value, speed ->
                moveVisual(this, anchors, value, speed)
                if (!arrived && abs(to - value) <= near) {
                    arrived = true
                    val landed = (latest as? SheetStop.At)?.detent ?: detent
                    arrive(landed)
                    if (byPerson && landed != from) {
                        if (haptic) host.haptic(AppHaptic.Threshold)
                        host.onDetentChanged(landed)
                    }
                }
            }
            seed = anchored.lastVelocity
        }
    } finally {
        animating = false
    }
    stretch = 0f
    if (!arrived) arrive(detent)
    if (!leaving) syncAnchors(SheetStop.At(currentDetent ?: detent))
}

/**
 * Rises from where the sheet is to [requested] (or its initial detent), on the present spring;
 * a sheet that was leaving turns around at the speed it had. A restored sheet or a preview arrives
 * without sliding.
 */
internal suspend fun AppSheetState.present(requested: SheetDetent?, turning: Boolean) {
    val host = host ?: return
    val restored = restoredDetent.takeIf { !turning }
    restoredDetent = null
    val stop = activeFor(requested ?: restored ?: initialDetent)
    heading(stop)
    presenting = true
    if (host.crossFade && !turning && restored == null) fade = 0f
    syncAnchors(SheetStop.At(stop).takeIf { restored != null } ?: anchored.settledValue)
    animating = true
    try {
        anchored.anchoredDrag(SheetStop.At(stop), MutatePriority.PreventUserInput) { anchors, latest ->
            val to = anchors.positionOf(latest)
            when {
                restored != null -> dragTo(to)
                host.crossFade -> {
                    dragTo(to)
                    animate(fade, 1f, animationSpec = host.springs.fadeIn()) { value, _ -> fade = value }
                }
                else -> {
                    fade = 1f
                    animate(anchored.offset, to, anchored.lastVelocity, host.presentSpring.pixels) { value, speed ->
                        dragTo(value, speed)
                    }
                }
            }
        }
    } finally {
        animating = false
        presenting = false
    }
    arrive(stop)
    syncAnchors(SheetStop.At(stop))
}

/**
 * Leaves for hidden at [velocity], from where the sheet shows, its rubber band included. The
 * decision is already made: [AppSheetState.beginLeaving] ran in its frame.
 */
internal suspend fun AppSheetState.exit(velocity: Float) {
    if (!leaving) beginLeaving()
    val host = host
    val stops = stops
    if (host == null || stops == null) {
        finishHidden()
        host?.onGone()
        return
    }
    syncAnchors(anchored.settledValue)
    animating = true
    try {
        anchored.anchoredDrag(SheetStop.Hidden, MutatePriority.PreventUserInput) { anchors, latest ->
            val to = anchors.positionOf(latest)
            when {
                host.crossFade -> {
                    animate(fade, 0f, animationSpec = host.springs.fadeOut()) { value, _ -> fade = value }
                    stretch = 0f
                    dragTo(to)
                }
                else -> {
                    val from = anchored.offset + stretch
                    animate(from, to, velocity, host.springs.sheet.pixels) { value, speed ->
                        moveVisual(this, anchors, value, speed)
                    }
                }
            }
        }
    } finally {
        animating = false
    }
    if (leaving) {
        finishHidden()
        host.onGone()
    }
}

/** A finger landed on a moving sheet: it stops where it is, at the priority code motion uses. */
internal suspend fun AppSheetState.stopMotion() {
    anchored.anchoredDrag(MutatePriority.PreventUserInput) {}
}

/**
 * Places the sheet at [value] along its travel: inside the anchors by moving them, past an edge by
 * the rubber band, so a settle out of a stretch is one continuous curve.
 */
private fun AppSheetState.moveVisual(scope: AnchoredDragScope, anchors: DraggableAnchors<SheetStop>, value: Float, speed: Float) {
    val min = anchors.minPosition()
    val max = anchors.maxPosition()
    val inside = value.coerceIn(min, max)
    stretch = value - inside
    scope.dragTo(inside, speed)
}

/** The rubber band a stretch of this sign is in. */
internal fun AppSheetState.bandFor(stretch: Float): SheetBand {
    val stops = stops ?: return SheetBand(0f)
    return SheetBand(if (stretch < 0f) stops.roomAbove else stops.hidden - stops.lowest)
}
