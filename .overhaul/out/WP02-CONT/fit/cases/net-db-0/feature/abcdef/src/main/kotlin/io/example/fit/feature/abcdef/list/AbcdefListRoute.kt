package io.example.fit.feature.abcdef.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdef.api.AbcdefDetailKey

@Composable
internal fun AbcdefListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefListScreen(state = state, onEvent = onEvent)
    }
}
