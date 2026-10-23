package io.example.fit.feature.abcdefghijkl.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijkl.api.AbcdefghijklDetailKey

@Composable
internal fun AbcdefghijklListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklListScreen(state = state, onEvent = onEvent)
    }
}
