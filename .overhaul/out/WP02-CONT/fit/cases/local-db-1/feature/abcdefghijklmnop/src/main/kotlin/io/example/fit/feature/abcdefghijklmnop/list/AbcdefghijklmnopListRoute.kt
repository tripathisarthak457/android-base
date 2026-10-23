package io.example.fit.feature.abcdefghijklmnop.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnop.api.AbcdefghijklmnopDetailKey

@Composable
internal fun AbcdefghijklmnopListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopListScreen(state = state, onEvent = onEvent)
    }
}
