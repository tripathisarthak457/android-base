package io.example.fit.feature.abcdefghijklmnopqrstuvw.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwListScreen(state = state, onEvent = onEvent)
    }
}
