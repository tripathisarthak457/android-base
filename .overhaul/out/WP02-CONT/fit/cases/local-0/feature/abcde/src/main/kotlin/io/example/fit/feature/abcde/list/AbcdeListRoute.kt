package io.example.fit.feature.abcde.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcde.api.AbcdeDetailKey

@Composable
internal fun AbcdeListRoute(
    navigator: AppNavigator,
    viewModel: AbcdeListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdeListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdeListEffect.OpenDetail -> {
                    navigator.navigate(AbcdeDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdeListScreen(state = state, onEvent = onEvent)
    }
}
