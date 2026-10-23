package io.example.fit.feature.abcdefghijklmnopq.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqDetailViewModel =
        hiltViewModel<AbcdefghijklmnopqDetailViewModel, AbcdefghijklmnopqDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqDetailScreen(state = state, onEvent = onEvent)
    }
}
