package com.base.app.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay

/**
 * Registered after the display, so it is asked first: while the screens move, Back is counted here
 * and a back swipe gets no half-seeked scene; its release counts as one press.
 */
@Composable
internal fun GateEffects(gate: BackGate, pop: (Int) -> Int) {
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    NavigationBackHandler(state = idle, isBackEnabled = gate.moving, onBackCompleted = { gate.onBack(pop) })
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
