package com.example.modulescompact.feature.orders.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulescompact.core.navigation.AppNavigator
import com.example.modulescompact.core.ui.MviScreen

@Composable
internal fun OrdersDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: OrdersDetailViewModel =
        hiltViewModel<OrdersDetailViewModel, OrdersDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(OrdersDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                OrdersDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        OrdersDetailScreen(state = state, onEvent = onEvent)
    }
}
