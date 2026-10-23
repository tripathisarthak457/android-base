package io.example.fit.feature.abcdefghijklmnopq.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopq.api.AbcdefghijklmnopqDetailKey

@Composable
internal fun AbcdefghijklmnopqListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqListScreen(state = state, onEvent = onEvent)
    }
}
