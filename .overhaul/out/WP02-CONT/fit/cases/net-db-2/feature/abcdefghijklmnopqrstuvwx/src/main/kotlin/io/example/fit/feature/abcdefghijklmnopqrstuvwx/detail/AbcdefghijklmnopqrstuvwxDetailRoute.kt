package io.example.fit.feature.abcdefghijklmnopqrstuvwx.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqrstuvwxDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxDetailViewModel =
        hiltViewModel<
            AbcdefghijklmnopqrstuvwxDetailViewModel,
            AbcdefghijklmnopqrstuvwxDetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqrstuvwxDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxDetailScreen(state = state, onEvent = onEvent)
    }
}
