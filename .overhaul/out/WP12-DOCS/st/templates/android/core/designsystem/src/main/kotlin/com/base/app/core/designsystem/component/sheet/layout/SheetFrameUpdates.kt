package com.base.app.core.designsystem.component.sheet.layout

import android.os.Build
import androidx.compose.animation.core.animate
import androidx.compose.foundation.MutatePriority
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.component.sheet.AppSheetState
import com.base.app.core.designsystem.component.sheet.SheetContentInteraction
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetModality
import com.base.app.core.designsystem.component.sheet.SheetPlacement
import com.base.app.core.designsystem.component.sheet.SheetStop
import com.base.app.core.designsystem.component.sheet.activeFor
import com.base.app.core.designsystem.component.sheet.syncAnchors
import com.base.app.core.designsystem.foundation.layout.ShellSeamApi
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/** The sheet's bounds in its container: its pane for a non-modal sheet, a hinge half, or all of it. */
internal fun SheetController.boundsWithin(width: Float, height: Float): Rect {
    val whole = Rect(0f, 0f, width, height)
    val pane = if (modality is SheetModality.Modal) null else paneRect()?.intersect(whole)
    val base = pane?.takeIf { it.width > 0f && it.height > 0f } ?: whole
    val half = hingeHalf?.intersect(base)
    return half?.takeIf { it.width > 0f && it.height > 0f } ?: base
}

/**
 * New stops from measure. A sheet keeps its detent by identity: on a new window size it snaps
 * to where that detent now is, or retargets the animation in flight; when only its content's height,
 * a resolver or the detent list changed under a resting sheet, it moves there on the sheet spring.
 */
internal fun SheetController.applyStops(new: SheetStops, inputs: DetentInputs) {
    val state = sheetState
    val old = state.stops
    val oldInputs = state.inputs
    state.inputs = inputs
    if (old == new) return
    state.stops = new
    updateBoundary(new, inputs)
    if (inspection && callerShows == true && !state.isVisible) {
        state.shown = true
        state.arrive(state.activeFor(state.initialDetent))
    }
    val keep = keepFor(old, new)
    val resized = oldInputs == null || old == null || old.single != new.single ||
        oldInputs.boundsHeight != inputs.boundsHeight || oldInputs.largeTop != inputs.largeTop ||
        oldInputs.restBottom != inputs.restBottom || oldInputs.sizeClass != inputs.sizeClass ||
        oldInputs.foldBottom != inputs.foldBottom
    val resting = state.isVisible && !state.isMoving && keep is SheetStop.At &&
        state.anchored.anchors.hasPositionFor(keep)
    if (resized || !resting) {
        state.syncAnchors(keep)
        return
    }
    scope.launch(start = CoroutineStart.UNDISPATCHED) { state.moveTo(keep.detent) }
}

private fun SheetController.keepFor(old: SheetStops?, new: SheetStops): SheetStop {
    val state = sheetState
    if (!state.isVisible || state.isLeaving) return SheetStop.Hidden
    // Shown but not yet rising: the present slides it up from hidden, so a layout in between must
    // not put it at its detent first.
    if (state.currentDetent == null && !state.presenting) return SheetStop.Hidden
    if (new.single) return SheetStop.At(new.detents[0])
    val heading = state.targetDetent ?: state.currentDetent ?: state.initialDetent
    if (old?.single == true) return SheetStop.At(if (new.has(heading)) heading else new.detents[0])
    return SheetStop.At(state.activeFor(heading))
}

/**
 * Moves a resting sheet to where [detent] now is. The new anchors go in while the mutex is held, so
 * they cannot snap the sheet; the block restarts with them and animates from where the sheet is.
 */
private suspend fun AppSheetState.moveTo(detent: SheetDetent) {
    val host = host ?: return
    heading(detent)
    animating = true
    try {
        anchored.anchoredDrag(SheetStop.At(detent), MutatePriority.Default) { anchors, latest ->
            syncAnchors(latest)
            animate(anchored.offset, anchors.positionOf(latest), anchored.lastVelocity, host.springs.sheet.pixels) { value, speed ->
                dragTo(value, speed)
            }
        }
    } finally {
        animating = false
    }
    arrive(detent)
}

/** For a non-modal sheet, the offsets between which its dim ramps in. */
private fun SheetController.updateBoundary(stops: SheetStops, inputs: DetentInputs) {
    val upThrough = (modality as? SheetModality.NonModal)?.upThrough
    if (upThrough == null || stops.single) {
        stacked.boundary = Float.NaN
        stacked.boundaryNext = Float.NaN
        return
    }
    val index = boundaryIndex(upThrough, stops, inputs)
    stacked.boundary = stops.offsets[index]
    stacked.boundaryNext = if (index > 0) stops.offsets[index - 1] else Float.NaN
}

/** Writes [frame] when it changed, and the flags the stack reads from it. */
internal fun SheetController.updateFrame(frame: SheetFrameInfo) {
    if (this.frame != frame) this.frame = frame
    val stops = sheetState.stops
    stacked.drivesCardStack = frame.recedes && properties.recedesPresenter && stops != null && stops.largeIndex >= 0
    stacked.spansWidth = frame.spansWidth && frame.isBottom
    stacked.attached = frame.isBottom && !frame.floats
    stacked.cornerRadius = frame.radius
    stacked.paneBounded = modality !is SheetModality.Modal &&
        (frame.boundsWidth < windowSize.width - 1f || frame.boundsHeight < windowSize.height - 1f)
}

/** The detent a form sheet or a side panel records for when the window returns it to the bottom edge. */
internal fun SheetController.recordedDetent(): SheetDetent = sheetState.currentDetent ?: sheetState.initialDetent

/** Large's top for a sheet stacked on another, in window pixels: the covered sheet's lifted top plus the peek. */
internal fun SheetController.stackedLargeTop(): Float {
    val stack = stack ?: return Float.NEGATIVE_INFINITY
    val below = stack.front(stacked.level - 1)?.takeIf { it !== stacked } ?: return Float.NEGATIVE_INFINITY
    val top = below.topStopInWindow()
    if (top.isNaN()) return Float.NEGATIVE_INFINITY
    return top - DesignTokens.Sheet.Stack.coveredLift * windowSize.height + DesignTokens.Sheet.Stack.peek.value * density
}

/** Whether the content scrolls before the sheet grows when nothing about the gesture is known yet. */
internal fun SheetController.scrollsAtRest(): Boolean = when (properties.contentInteraction) {
    SheetContentInteraction.Scrolls -> true
    SheetContentInteraction.Resizes -> false
    else -> modality !is SheetModality.Modal || touchExploration
}

/** Reads the keyboard in placement and lifts the sheet while focus is in it. */
internal fun SheetController.followKeyboard(scope: Placeable.PlacementScope, frame: SheetFrameInfo) {
    val height = windowSize.height
    val keyboard = with(imeSource) { scope.current(height) }
    val target = with(imeSource) { scope.target(height) }
    if (imeHeight != keyboard) imeHeight = keyboard
    if (imeTarget != target) imeTarget = target
    val state = sheetState
    val y = state.anchored.offset
    val lift = when {
        !focusInside || keyboard <= 0f || y.isNaN() -> 0f
        frame.isBottom -> (keyboard - frame.bottomInset).coerceIn(0f, (y - frame.largeTop).coerceAtLeast(0f))
        frame.placement == SheetPlacement.Center -> {
            val gap = DesignTokens.Sheet.Keyboard.formLiftGap.value * density
            val cardBottom = y + frame.cardHeight
            (cardBottom - (frame.boundsHeight - keyboard) + gap).coerceIn(0f, (y - frame.largeTop + DesignTokens.Sheet.Side.inset.value * density).coerceAtLeast(0f))
        }
        else -> 0f
    }
    // Before API 30 the keyboard's inset jumps with no animation to follow; the lift then springs toward it.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (state.lift != lift) state.lift = lift
    } else if (liftGoal != lift) {
        liftGoal = lift
    }
}

/** A sheet resting on the tab bar rides it while it minimises, less as the sheet expands over it. */
internal fun SheetController.followBar(frame: SheetFrameInfo) {
    val state = sheetState
    @OptIn(ShellSeamApi::class)
    val bar = measureEnvironment?.barFrame
    val ride = if (frame.onBar && bar != null) (bar.liveTop - bar.settledTop) * (1f - state.expansionFraction) else 0f
    if (state.barRide != ride) state.barRide = ride
}

/** Whether the scrim is placed and blocks what is behind: always for a modal sheet that shows. */
internal fun SheetController.blocksBeneath(): Boolean {
    val state = sheetState
    val blocks = state.isVisible && !state.parkedInstantly && state.parkProgress < 1f &&
        (modality is SheetModality.Modal || stacked.ramp() > 0f)
    if (blocking != blocks) blocking = blocks
    return blocks
}

/** The surface's bottom edge in bounds pixels this frame. */
internal fun SheetController.surfaceBottomInBounds(): Float {
    val frame = frame ?: return 0f
    val state = sheetState
    val placed = state.placedTop()
    return when {
        frame.isCard -> placed + frame.cardHeight
        !frame.isBottom -> frame.restBottom
        floatingNow -> {
            val resting = SheetGeometry.restingBottom(frame.boundsHeight, frame.restBottom, frame.inset, true, frame.radius, frame.onBar)
            lerp(resting, frame.boundsHeight + frame.radius, state.expansionFraction)
        }
        frame.onBar -> lerp(frame.restBottom, frame.boundsHeight + frame.radius, state.expansionFraction)
        else -> frame.boundsHeight + frame.radius
    }
}

/** How far down the sheet layer its surface shows: the predictive back shrink's pivot. */
internal fun SheetController.visibleBottomInLayer(): Float {
    val frame = frame ?: return 0f
    return minOf(frame.boundsHeight, surfaceBottomInBounds()) - sheetState.placedTop()
}

/** The footer's top in layer pixels: on the visible bottom or the keyboard, never above the header. */
internal fun SheetController.footerTopInLayer(footerHeight: Float): Float {
    val frame = frame ?: return 0f
    val placed = sheetState.placedTop()
    val bottom = minOf(frame.restBottom, surfaceBottomInBounds(), frame.boundsHeight - imeHeight)
    val top = maxOf(bottom - footerHeight, placed + frame.bandHeight)
    return top - placed
}

/** The expansion of the sheet stacked on this one, which recedes it. */
internal fun SheetController.coverExpansion(): Float = stack?.coverOf(stacked)?.expansion() ?: 0f

/** More than two sheets are up in front of this one: it is not drawn. */
internal fun SheetController.hiddenBehindStack(): Boolean =
    (stack?.depthAbove(stacked) ?: 0) > DesignTokens.Sheet.Stack.maxCoveredDrawn

/** Applies the owner entry's own transform this frame, for a sheet that moves with its owner. */
@OptIn(ShellSeamApi::class)
internal fun SheetController.applyEntryPose(scope: GraphicsLayerScope) {
    val frame = frame ?: return
    entry.pose.applyTo(scope, Offset(frame.boundsLeft + frame.sheetLeft, sheetState.boundsTop + sheetState.placedTop()))
}
