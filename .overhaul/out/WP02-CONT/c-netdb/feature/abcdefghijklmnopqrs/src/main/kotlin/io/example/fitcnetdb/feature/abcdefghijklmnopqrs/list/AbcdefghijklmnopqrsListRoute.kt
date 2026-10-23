package io.example.fitcnetdb.feature.abcdefghijklmnopqrs.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnetdb.core.navigation.AppNavigator
import io.example.fitcnetdb.core.ui.MviScreen
import io.example.fitcnetdb.feature.abcdefghijklmnopqrs.api.AbcdefghijklmnopqrsDetailKey

@Composable
internal fun AbcdefghijklmnopqrsListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnopqrsListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnopqrsListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnopqrsDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnopqrsListScreen(state = state, onEvent = onEvent)
    }
}
