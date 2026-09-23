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

    // Keeps the latest lambda without restarting the collector on every recomposition.
    val currentOnEffect by rememberUpdatedState(onEffect)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Main.immediate handles an effect in the frame it arrives, so a STOP cannot drop it.
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
