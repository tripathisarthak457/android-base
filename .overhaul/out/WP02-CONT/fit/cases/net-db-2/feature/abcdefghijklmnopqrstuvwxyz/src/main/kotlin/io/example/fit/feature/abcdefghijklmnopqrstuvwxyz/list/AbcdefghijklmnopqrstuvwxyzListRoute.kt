package io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwxyzListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwxyzDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzListScreen(state = state, onEvent = onEvent)
    }
}
