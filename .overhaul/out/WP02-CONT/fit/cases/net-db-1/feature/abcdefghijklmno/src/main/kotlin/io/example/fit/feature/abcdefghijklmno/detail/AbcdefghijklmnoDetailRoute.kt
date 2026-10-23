package io.example.fit.feature.abcdefghijklmno.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen

@Composable
internal fun AbcdefghijklmnoDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnoDetailViewModel =
        hiltViewModel<AbcdefghijklmnoDetailViewModel, AbcdefghijklmnoDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnoDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghijklmnoDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnoDetailScreen(state = state, onEvent = onEvent)
    }
}
