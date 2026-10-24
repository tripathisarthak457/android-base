package com.base.app.core.designsystem.component.sheet.layout

import android.os.Build
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.stack.SheetStack
import com.base.app.core.designsystem.foundation.layout.LocalHostMotion
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The sheet's place in its window's stack while it shows. Leaving composition while
 * shown takes it out in that frame, so its presenter straightens and its dim goes at once.
 */
@Composable
internal fun SheetStackEntry(controller: SheetController, stack: SheetStack?, ownerLevel: Int) {
    if (stack == null) return
    DisposableEffect(controller, stack) {
        controller.stack = stack
        onDispose {
            stack.unregister(controller.stacked)
            controller.stack = null
        }
    }
    LaunchedEffect(controller, stack, ownerLevel) {
        snapshotFlow { controller.sheetState.isVisible }.collect { visible ->
            if (visible) stack.register(controller.stacked, ownerLevel) else stack.unregister(controller.stacked)
        }
    }
}

/**
 * The motion the sheet runs for its surroundings: parking while its owner is covered, the
 * reports its host counts as overlay motion, the keyboard's Expand and, before API 30,
 * the lift that springs toward a keyboard that jumped. It also follows TalkBack's touch
 * exploration, under which a sheet's content scrolls before the sheet grows.
 */
@Composable
internal fun SheetMotionEffects(controller: SheetController, expandForKeyboard: Boolean) {
    val state = controller.sheetState
    val hostMotion = LocalHostMotion.current
    LaunchedEffect(controller, expandForKeyboard) {
        launch {
            snapshotFlow { controller.entry.isTabSelected && !controller.entry.isUncovered && !state.isLeaving }
                .collectLatest { covered ->
                    val target = if (covered) 1f else 0f
                    if (state.parkProgress != target) {
                        animate(state.parkProgress, target, animationSpec = controller.springs.sheet.unit) { value, _ ->
                            state.parkProgress = value
                        }
                    }
                }
        }
        if (expandForKeyboard) {
            launch {
                var before: SheetDetent? = null
                snapshotFlow { controller.imeTarget > 0f && controller.focusInside }.collect { up ->
                    val stops = state.stops ?: return@collect
                    if (up) {
                        before = state.currentDetent
                        state.animateTo(stops.detents[0])
                    } else {
                        val back = before
                        before = null
                        if (back != null && state.currentDetent == stops.detents[0]) state.animateTo(back)
                    }
                }
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            launch {
                snapshotFlow { controller.liftGoal }.collectLatest { goal ->
                    animate(state.lift, goal, animationSpec = controller.springs.screen.pixels) { value, _ -> state.lift = value }
                }
            }
        }
    }
    DisposableEffect(controller, hostMotion) {
        controller.hostMotion = hostMotion
        onDispose {
            hostMotion.report(state, false)
            if (controller.hostMotion === hostMotion) controller.hostMotion = null
        }
    }
    LaunchedEffect(controller, hostMotion) {
        snapshotFlow { controller.movesHost() }.collect { moving -> hostMotion.report(state, moving) }
    }
    val context = LocalContext.current
    val accessibility = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    DisposableEffect(controller, accessibility) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { controller.touchExploration = it }
        controller.touchExploration = accessibility?.isTouchExplorationEnabled == true
        accessibility?.addTouchExplorationStateChangeListener(listener)
        onDispose { accessibility?.removeTouchExplorationStateChangeListener(listener) }
    }
}

/**
 * The sheet moves its host's content while it shows, hides or parks, until 99 % of that travel
 * is done. Drags and settles move only the sheet.
 */
private fun SheetController.movesHost(): Boolean {
    val state = sheetState
    val stops = state.stops ?: return false
    val travel = stops.hidden - stops.lowest
    if (travel <= 0f) return false
    val offset = state.anchored.offset
    if (offset.isNaN()) return false
    val parking = state.parkProgress > Remaining && state.parkProgress < 1f - Remaining
    val showing = state.presenting && abs(offset - (state.targetDetent?.let { stops.offsetOf(it) } ?: offset)) / travel > Remaining
    val leaving = state.isLeaving && (stops.hidden - offset) / travel > Remaining
    return parking || showing || leaving
}

/** The share of a host-moving motion left when the host may act again: what the back gate waits for. */
private const val Remaining = 1f - DesignTokens.Motion.navigationBackGateRelease
