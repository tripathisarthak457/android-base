package io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.audit.core.navigation.AppNavigator
import io.example.audit.core.ui.MviScreen
import io.example.audit.feature.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzabcdListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzabcdListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcdListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwxyzabcdListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwxyzabcdDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzabcdListScreen(state = state, onEvent = onEvent)
    }
}
