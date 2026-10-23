package io.example.fit.feature.abcdefghijklmnopqrstuvwx.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwxListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwxListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwxDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxListScreen(state = state, onEvent = onEvent)
    }
}
