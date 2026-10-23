package io.example.fitcnet.feature.abcdefghijklmnopqrs.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnet.core.navigation.AppNavigator
import io.example.fitcnet.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqrsDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrsDetailViewModel =
        hiltViewModel<
            AbcdefghijklmnopqrsDetailViewModel,
            AbcdefghijklmnopqrsDetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrsDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqrsDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrsDetailScreen(state = state, onEvent = onEvent)
    }
}
