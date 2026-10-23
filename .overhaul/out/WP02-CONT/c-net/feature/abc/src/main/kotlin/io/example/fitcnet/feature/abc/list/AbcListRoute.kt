package io.example.fitcnet.feature.abc.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.fitcnet.core.navigation.AppNavigator
import io.example.fitcnet.core.ui.MviScreen
import io.example.fitcnet.feature.abc.api.AbcDetailKey

@Composable
internal fun AbcListRoute(navigator: AppNavigator, viewModel: AbcListViewModel = hiltViewModel()) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(AbcListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is AbcListEffect.OpenDetail -> {
                    navigator.navigate(AbcDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        AbcListScreen(state = state, onEvent = onEvent)
    }
}
