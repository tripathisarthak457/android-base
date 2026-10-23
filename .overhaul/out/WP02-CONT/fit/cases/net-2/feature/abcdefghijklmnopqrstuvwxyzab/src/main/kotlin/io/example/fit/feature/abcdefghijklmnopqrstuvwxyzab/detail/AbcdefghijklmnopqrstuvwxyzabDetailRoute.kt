package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzabDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzabDetailViewModel =
        hiltViewModel<
            AbcdefghijklmnopqrstuvwxyzabDetailViewModel,
            AbcdefghijklmnopqrstuvwxyzabDetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqrstuvwxyzabDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzabDetailScreen(state = state, onEvent = onEvent)
    }
}
