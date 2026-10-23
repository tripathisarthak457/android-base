package io.example.fit.feature.abcdefghij.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghij.api.AbcdefghijDetailKey

@Composable
internal fun AbcdefghijListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijListScreen(state = state, onEvent = onEvent)
    }
}
