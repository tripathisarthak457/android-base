package io.example.audit.feature.order.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.example.audit.core.navigation.AppNavigator
import io.example.audit.core.ui.MviScreen

@Composable
internal fun OrderDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: OrderDetailViewModel =
        hiltViewModel<OrderDetailViewModel, OrderDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    // Once, when the screen enters composition. Sending an event suspends nothing, so it needs no
    // coroutine of its own.
    SideEffect(Unit) { viewModel.onEvent(OrderDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                OrderDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        OrderDetailScreen(state = state, onEvent = onEvent)
    }
}
