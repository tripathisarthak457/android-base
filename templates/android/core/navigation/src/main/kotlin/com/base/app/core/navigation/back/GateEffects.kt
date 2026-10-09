package com.base.app.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay

/**
 * For a host drawn by `NavDisplay` (the presentation host): registered after the display, so it is
 * asked first while the screens move. Back is counted here, and a back swipe gets no half-seeked
 * scene; its release counts as one press.
 */
@Composable
internal fun GateEffects(gate: BackGate, pop: (Int) -> Int) {
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = idle, isBackEnabled = gate.moving, onBackCompleted = { gate.onBack(pop) })
    GateSettle(gate, pop)
}

/** Applies the presses [gate] counted, as one pop, once the screens stop. */
@Composable
internal fun GateSettle(gate: BackGate, pop: (Int) -> Int) {
    LaunchedEffect(gate) {
        snapshotFlow { gate.moving }.collect { moving -> if (!moving) gate.settle(pop) }
    }
    // A change that starts no transition (reduce motion, a destination with none) must not keep
    // Back counted forever.
    val changes = gate.changes
    LaunchedEffect(gate, changes) {
        delay(MOTION_START_WINDOW_MS)
        gate.noMotion()
    }
}

/** How long a stack change may take to start its transition before Back stops being counted. */
private const val MOTION_START_WINDOW_MS = 150L
