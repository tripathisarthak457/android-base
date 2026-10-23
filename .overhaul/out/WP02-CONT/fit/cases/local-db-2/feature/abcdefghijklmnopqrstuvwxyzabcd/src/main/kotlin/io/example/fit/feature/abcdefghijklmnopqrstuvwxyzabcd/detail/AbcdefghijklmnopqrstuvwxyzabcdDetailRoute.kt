package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabcd.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzabcdDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel =
        hiltViewModel<
            AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel,
            AbcdefghijklmnopqrstuvwxyzabcdDetailViewModel.Factory,
            >(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcdDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnopqrstuvwxyzabcdDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzabcdDetailScreen(state = state, onEvent = onEvent)
    }
}
