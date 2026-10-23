package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabDetailKey

@Composable
internal fun AbcdefghijklmnopqrstuvwxyzabListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrstuvwxyzabListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrstuvwxyzabListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrstuvwxyzabDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrstuvwxyzabListScreen(state = state, onEvent = onEvent)
    }
}
