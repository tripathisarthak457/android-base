package io.example.fit.feature.abcdefghijklmnopqrst.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrst.api.AbcdefghijklmnopqrstDetailKey

@Composable
internal fun AbcdefghijklmnopqrstListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstListScreen(state = state, onEvent = onEvent)
    }
}
