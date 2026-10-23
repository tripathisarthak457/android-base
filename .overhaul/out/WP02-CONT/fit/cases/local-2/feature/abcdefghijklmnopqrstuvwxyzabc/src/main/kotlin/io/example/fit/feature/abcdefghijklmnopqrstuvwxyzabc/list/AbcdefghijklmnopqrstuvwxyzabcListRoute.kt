package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.api.AbcdefghijklmnopqrstuvwxyzabcDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzabcListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzabcListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwxyzabcListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwxyzabcDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzabcListScreen(state = state, onEvent = onEvent)
    }
}
