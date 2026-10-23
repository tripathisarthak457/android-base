package io.example.fit.feature.abcdefghijklmnopqrstuv.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuv.api.AbcdefghijklmnopqrstuvDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvListScreen(state = state, onEvent = onEvent)
    }
}
