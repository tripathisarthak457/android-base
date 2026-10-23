package io.example.fit.feature.abcdefghijklmnopqrst.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqrstDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstDetailViewModel =
        hiltViewModel<
            AbcdefghijklmnopqrstDetailViewModel,
            AbcdefghijklmnopqrstDetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqrstDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstDetailScreen(state = state, onEvent = onEvent)
    }
}
