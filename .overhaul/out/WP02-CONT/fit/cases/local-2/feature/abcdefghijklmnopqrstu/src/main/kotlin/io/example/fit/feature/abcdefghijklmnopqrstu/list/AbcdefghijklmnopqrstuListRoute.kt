package io.example.fit.feature.abcdefghijklmnopqrstu.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuListScreen(state = state, onEvent = onEvent)
    }
}
