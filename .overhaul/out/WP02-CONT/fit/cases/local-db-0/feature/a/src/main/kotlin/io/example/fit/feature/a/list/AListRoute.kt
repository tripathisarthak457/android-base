package io.example.fit.feature.a.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.a.api.ADetailKey

@Composable
internal fun AListRoute(navigator: AppNavigator, viewModel: AListViewModel = hiltViewModel()) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AListEffect.OpenDetail -> {
                    navigator.navigate(ADetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AListScreen(state = state, onEvent = onEvent)
    }
}
