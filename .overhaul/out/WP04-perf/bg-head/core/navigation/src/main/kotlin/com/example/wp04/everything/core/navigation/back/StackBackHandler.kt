package com.example.wp04.everything.core.navigation.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import com.example.wp04.everything.core.navigation.AppBackStack
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
    val scope = rememberCoroutineScope()
    val handler = remember(stack, host, pop, scope) { StackBackEvents(stack.gate, host, pop, scope) }
    SideEffect { handler.cardEnabled = cardEnabled }
    BackHandlerEffect(handler) { stack.canGoBack || stack.gate.moving }
}

private class StackBackEvents(
    private val gate: BackGate,
    private val host: BackCardHost,
    private val pop: (Int) -> Int,
    private val scope: CoroutineScope,
) : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, isBackEnabled = false) {

    var cardEnabled = true

    override fun onBackStarted(event: NavigationEvent) {
        BackTrace.phase(1)
        follow(event)
    }

    override fun onBackProgressed(event: NavigationEvent) = follow(event)

    private fun follow(event: NavigationEvent) {
        if (!cardEnabled) return
        val card = host.card
        when (card.phase) {
            BackCardState.Phase.Tracking -> card.track(event)
            // A button press can arrive as progress animated by the system: it never shrinks the screen.
            BackCardState.Phase.Idle ->
                if (event.swipeEdge != NavigationEvent.EDGE_NONE && host.canStartCard()) host.startCard(event, scope)
            BackCardState.Phase.Settling -> Unit
        }
    }

    override fun onBackCancelled() {
        BackTrace.phase(3)
        if (host.card.phase == BackCardState.Phase.Tracking) scope.launch { host.cancelCard() }
    }

    override fun onBackCompleted() {
        BackTrace.phase(2)
        if (host.card.phase == BackCardState.Phase.Tracking) {
            scope.launch { host.commitCard(pop, scope) }
        } else {
            gate.onBack(pop)
        }
    }
}
