package io.example.fitcnet.feature.abcdefghi.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnet.core.navigation.AppNavigator
import io.example.fitcnet.core.ui.MviScreen
import io.example.fitcnet.feature.abcdefghi.api.AbcdefghiDetailKey

@Composable
internal fun AbcdefghiListRoute(
    navigator: AppNavigator,
    viewModel: AbcdefghiListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcdefghiListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcdefghiListEffect.OpenDetail -> {
                    navigator.navigate(AbcdefghiDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcdefghiListScreen(state = state, onEvent = onEvent)
    }
}
