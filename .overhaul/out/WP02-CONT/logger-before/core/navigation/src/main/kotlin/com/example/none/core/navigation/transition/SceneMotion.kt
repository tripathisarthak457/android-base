package com.example.none.core.navigation.transition

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.example.none.core.navigation.back.BackGate

/**
 * How far a scene or entry has arrived: 0 off screen, 1 in place, on the spring its slide uses, so
 * anything driven by it (the scrim, the covered screen following the parallax, the tab bar) moves
 * in step with the slide without reading the slide itself.
 */
@Composable
internal fun Transition<EnterExitState>.arrival(motion: ScreenMotion): State<Float> =
    animateFloat(transitionSpec = { motion.fraction }, label = "arrival") {
        if (it == EnterExitState.Visible) 1f else 0f
    }

/**
 * Tells [gate] while this scene's transition is short of settled. That includes a back swipe in
 * progress: the swipe stays with the handler that took it (navigationevent finishes a started
 * gesture on its own handler), and a Back pressed meanwhile is counted rather than applied under it.
 *
 * The threshold is on the distance travelled rather than on play time, which Compose does not
 * expose: at [BackGate.SETTLED_ENOUGH] the arriving screen is within a few pixels of its place, so
 * the pop that the counted presses start next cannot be told from one that waited for the end.
 */
@Composable
internal fun ReportMotion(transition: Transition<EnterExitState>, arrival: State<Float>, gate: BackGate) {
    val reporter = remember { Any() }
    LaunchedEffect(transition, arrival, gate) {
        snapshotFlow {
            val travelled = if (transition.targetState == EnterExitState.Visible) arrival.value else 1f - arrival.value
            transition.isRunning && travelled < BackGate.SETTLED_ENOUGH
        }.collect { gate.report(reporter, it) }
    }
    DisposableEffect(gate) {
        onDispose { gate.report(reporter, false) }
    }
}
