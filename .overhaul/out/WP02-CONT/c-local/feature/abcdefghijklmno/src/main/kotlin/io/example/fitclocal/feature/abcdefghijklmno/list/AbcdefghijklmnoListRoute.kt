package io.example.fitclocal.feature.abcdefghijklmno.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitclocal.core.navigation.AppNavigator
import io.example.fitclocal.core.ui.MviScreen
import io.example.fitclocal.feature.abcdefghijklmno.api.AbcdefghijklmnoDetailKey

@Composable
internal fun AbcdefghijklmnoListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnoListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnoListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnoListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnoDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnoListScreen(state = state, onEvent = onEvent)
    }
}
