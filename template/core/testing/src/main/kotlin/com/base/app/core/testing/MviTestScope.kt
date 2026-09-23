package com.base.app.core.testing

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.turbineScope
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiMessage
import com.base.app.core.common.mvi.UiState
import kotlinx.coroutines.test.runTest

/** The three things a ViewModel emits, collected together for the duration of a test. */
class MviTestScope<S : UiState, E : UiEvent, F : UiEffect>(
    private val viewModel: MviViewModel<S, E, F>,
    val states: ReceiveTurbine<S>,
    val effects: ReceiveTurbine<F>,
    val messages: ReceiveTurbine<UiMessage>,
) {

    fun send(event: E) = viewModel.onEvent(event)

    /** The next state. Fails the test if none arrives. */
    suspend fun state(): S = states.awaitItem()

    /** The state after the queue has caught up: the last one emitted, discarding those in front. */
    suspend fun settledState(): S {
        states.awaitItem()
        return states.expectMostRecentItem()
    }

    suspend fun effect(): F = effects.awaitItem()

    suspend fun message(): UiMessage = messages.awaitItem()

    /** Asserts nothing else is waiting — the counterpart to asserting something is. */
    suspend fun expectNoMoreEffects() = effects.expectNoEvents()
}

/**
 * Runs [block] against a ViewModel with its state, effects and messages already being collected.
 *
 * ```
 * @Test
 * fun `a failed load shows an error`() = viewModel.test {
 *     send(Event.Load)
 *     assertTrue(settledState().loadState is LoadState.Error)
 * }
 * ```
 */
fun <S : UiState, E : UiEvent, F : UiEffect> MviViewModel<S, E, F>.test(
    block: suspend MviTestScope<S, E, F>.() -> Unit,
) = runTest {
    turbineScope {
        val states = state.testIn(this)
        val effects = effects.testIn(this)
        val messages = messages.testIn(this)

        // The StateFlow replays its current value to every collector; dropping it here is what
        // makes `state()` mean "what changed" rather than "what it already was".
        states.awaitItem()

        MviTestScope(this@test, states, effects, messages).block()

        states.cancelAndIgnoreRemainingEvents()
        effects.cancelAndIgnoreRemainingEvents()
        messages.cancelAndIgnoreRemainingEvents()
    }
}
