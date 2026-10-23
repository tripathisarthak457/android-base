package io.example.fitcnetdb.feature.abcdefghi.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnetdb.core.navigation.AppNavigator
import io.example.fitcnetdb.core.ui.MviScreen

@Composable
internal fun AbcdefghiDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: AbcdefghiDetailViewModel =
        hiltViewModel<AbcdefghiDetailViewModel, AbcdefghiDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghiDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                AbcdefghiDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        AbcdefghiDetailScreen(state = state, onEvent = onEvent)
    }
}
