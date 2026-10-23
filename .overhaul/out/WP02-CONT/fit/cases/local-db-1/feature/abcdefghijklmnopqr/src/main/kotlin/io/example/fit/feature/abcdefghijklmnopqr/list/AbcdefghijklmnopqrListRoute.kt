package io.example.fit.feature.abcdefghijklmnopqr.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqr.api.AbcdefghijklmnopqrDetailKey

@Composable
internal fun AbcdefghijklmnopqrListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrListScreen(state = state, onEvent = onEvent)
    }
}
