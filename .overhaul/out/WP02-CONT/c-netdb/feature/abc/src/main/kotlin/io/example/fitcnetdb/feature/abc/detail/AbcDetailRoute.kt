package io.example.fitcnetdb.feature.abc.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnetdb.core.navigation.AppNavigator
import io.example.fitcnetdb.core.ui.MviScreen

@Composable
internal fun AbcDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcDetailViewModel =
        hiltViewModel<AbcDetailViewModel, AbcDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcDetailScreen(state = state, onEvent = onEvent)
    }
}
