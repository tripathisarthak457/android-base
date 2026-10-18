package com.base.app.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.base.app.core.navigation.AppBackStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What draws a host's [BackCard], and decides when the screen on top may become one. */
internal interface BackCardHost {
    val card: BackCardState

    fun canStartCard(): Boolean

    fun startCard(event: NavigationEvent, scope: CoroutineScope)

    suspend fun cancelCard()

    /** The card's commit: it fades, the stack pops with no slide, and Back pressed meanwhile is counted. */
    suspend fun commitCard(pop: (Int) -> Int, scope: CoroutineScope)
}

/**
 * The one Back handler of a stack host. A back gesture over a settled stack, or over a push still
 * sliding in, drives the [BackCard]; Back while anything else moves is counted by the [BackGate]
 * and applied once the screens stop, so a swipe started during a pop seeks nothing. The Back key,
 * the 3-button bar and gestures before API 34 send no progress, so they pop with the slide.
 *
 * Disabled at the root while nothing moves, so the shell's handler or the system (back to home)
 * takes Back there.
 */
@Composable
internal fun StackBackHandler(
    stack: AppBackStack,
    host: BackCardHost,
    pop: (Int) -> Int,
    cardEnabled: Boolean,
) {
    val gate = stack.gate
    val card = host.card
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state, host, cardEnabled) {
        if (!cardEnabled) return@LaunchedEffect
        snapshotFlow { state.transitionState }.collect { transition ->
            if (transition !is NavigationEventTransitionState.InProgress) return@collect
            val event = transition.latestEvent
            when (card.phase) {
                BackCardState.Phase.Tracking -> card.track(event)
                // A button press can arrive as progress animated by the system: it never shrinks the screen.
                BackCardState.Phase.Idle ->
                    if (event.swipeEdge != NavigationEvent.EDGE_NONE && host.canStartCard()) host.startCard(event, scope)
                BackCardState.Phase.Settling -> Unit
            }
        }
    }
    val enabled by remember(stack, gate) { derivedStateOf { stack.canGoBack || gate.moving } }
    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCancelled = {
            if (card.phase == BackCardState.Phase.Tracking) scope.launch { host.cancelCard() }
        },
        onBackCompleted = {
            if (card.phase == BackCardState.Phase.Tracking) {
                scope.launch { host.commitCard(pop, scope) }
            } else {
                gate.onBack(pop)
            }
        },
    )
}
