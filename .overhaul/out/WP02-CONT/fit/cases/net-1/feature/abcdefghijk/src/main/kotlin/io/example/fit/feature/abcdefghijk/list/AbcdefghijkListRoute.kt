package io.example.fit.feature.abcdefghijk.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijk.api.AbcdefghijkDetailKey

@Composable
internal fun AbcdefghijkListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijkListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijkListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijkListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijkDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijkListScreen(state = state, onEvent = onEvent)
    }
}
