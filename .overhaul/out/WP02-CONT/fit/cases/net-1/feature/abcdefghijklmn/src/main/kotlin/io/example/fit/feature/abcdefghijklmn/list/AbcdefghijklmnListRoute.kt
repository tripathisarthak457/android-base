package io.example.fit.feature.abcdefghijklmn.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefghijklmn.api.AbcdefghijklmnDetailKey

@Composable
internal fun AbcdefghijklmnListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghijklmnListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghijklmnListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghijklmnListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghijklmnDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghijklmnListScreen(state = state, onEvent = onEvent)
    }
}
