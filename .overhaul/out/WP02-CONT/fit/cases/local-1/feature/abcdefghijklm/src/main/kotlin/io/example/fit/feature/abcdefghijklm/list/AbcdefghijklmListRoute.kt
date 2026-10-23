package io.example.fit.feature.abcdefghijklm.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklm.api.AbcdefghijklmDetailKey

@Composable
internal fun AbcdefghijklmListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmListScreen(state = state, onEvent = onEvent)
    }
}
