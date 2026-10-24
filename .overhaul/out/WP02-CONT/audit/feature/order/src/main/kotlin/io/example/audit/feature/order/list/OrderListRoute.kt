package io.example.audit.feature.order.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.audit.core.navigation.AppNavigator
import io.example.audit.core.ui.MviScreen
import io.example.audit.feature.order.api.OrderDetailKey

@Composable
internal fun OrderListRoute(
    navigator: AppNavigator,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(OrderListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is OrderListEffect.OpenDetail -> {
                    navigator.navigate(OrderDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        OrderListScreen(state = state, onEvent = onEvent)
    }
}
