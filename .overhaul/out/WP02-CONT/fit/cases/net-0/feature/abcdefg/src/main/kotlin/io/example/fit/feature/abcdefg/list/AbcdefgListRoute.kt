package io.example.fit.feature.abcdefg.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fit.core.navigation.AppNavigator
import io.example.fit.core.ui.MviScreen
import io.example.fit.feature.abcdefg.api.AbcdefgDetailKey

@Composable
internal fun AbcdefgListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefgListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefgListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefgListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefgDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefgListScreen(state = state, onEvent = onEvent)
    }
}
