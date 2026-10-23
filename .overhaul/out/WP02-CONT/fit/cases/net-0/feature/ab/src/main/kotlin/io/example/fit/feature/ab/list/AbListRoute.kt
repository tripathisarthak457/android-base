package io.example.fit.feature.ab.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.ab.api.AbDetailKey

@Composable
internal fun AbListRoute(navigator: AppNavigator, viewModel: AbListViewModel = hiltViewModel()) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbListEffect.OpenDetail -> {
                    navigator.navigate(AbDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbListScreen(state = state, onEvent = onEvent)
    }
}
