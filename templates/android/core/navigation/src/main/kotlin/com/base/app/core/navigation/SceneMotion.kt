package com.base.app.core.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

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
 * Tells [gate] while this scene's transition is short of settled. A transition being seeked by a
 * back gesture is not "moving" here: the gesture owns it, and its release arrives as one Back.
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
            transition.isRunning && !transition.isSeeking && travelled < BackGate.SETTLED_ENOUGH
        }.collect { gate.report(reporter, it) }
    }
    DisposableEffect(gate) {
        onDispose { gate.report(reporter, false) }
    }
}
