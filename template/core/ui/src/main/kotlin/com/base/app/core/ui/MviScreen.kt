package com.base.app.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The bridge between a [MviViewModel] and its screen.
 *
 * Every route composable in the project is this call plus a screen composable. It exists so the
 * three things that are easy to get subtly wrong are done once, correctly:
 *
 * ## State is collected with the lifecycle
 *
 * `collectAsStateWithLifecycle` stops collecting when the screen goes to the background. A plain
 * `collectAsState` keeps an active collector on a screen nobody can see, and for state backed by
 * a `stateIn(WhileSubscribed)` upstream that means the work behind it never stops either.
 *
 * ## Effects are collected only while STARTED
 *
 * A bare `LaunchedEffect` collecting one-shot effects keeps running in the background. A
 * navigation effect emitted then fires immediately — from a screen nobody is looking at — and the
 * user returns to the app somewhere they never asked to go. `repeatOnLifecycle(STARTED)` cancels
 * the collector on STOP and restarts it on START; the effects channel buffers in the meantime, so
 * nothing is lost, it is only deferred until it can be acted on safely. Collection runs on
 * `Dispatchers.Main.immediate`, which closes the one gap a channel leaves: an effect received and
 * then dropped because the lifecycle stopped before it was dispatched.
 *
 * ## Messages have a host
 *
 * Every screen gets the snackbar host, so a repository failure is never silent because somebody
 * forgot to add one to this particular screen.
 *
 * ```
 * @Composable
 * fun SampleRoute(viewModel: SampleViewModel = hiltViewModel()) {
 *     MviScreen(viewModel, onEffect = { effect -> … }) { state, onEvent ->
 *         SampleScreen(state, onEvent)
 *     }
 * }
 * ```
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> MviScreen(
    viewModel: MviViewModel<S, E, F>,
    modifier: Modifier = Modifier,
    onEffect: (F) -> Unit = {},
    content: @Composable (state: S, onEvent: (E) -> Unit) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Kept up to date without restarting the collector: a recomposition that produces a new
    // lambda — which is every recomposition, for a lambda written inline at the call site —
    // would otherwise tear down and rebuild the subscription on every frame.
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate, not the frame clock Compose would otherwise use: an effect taken off
            // the channel is handled in the same frame, so a STOP arriving before the next frame
            // cannot cancel the collector with a navigation already received and never acted on.
            withContext(Dispatchers.Main.immediate) {
                viewModel.effects.collect { effect -> currentOnEffect(effect) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content(state, viewModel::onEvent)
        MessageHost(viewModel.messages)
    }
}
