package com.example.modulesmodular.feature.orders.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.ui.MviScreen
import com.example.modulesmodular.feature.orders.api.OrdersDetailKey

@Composable
internal fun OrdersListRoute(
    navigator: AppNavigator,
    viewModel: OrdersListViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(OrdersListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is OrdersListEffect.OpenDetail -> {
                    navigator.navigate(OrdersDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        OrdersListScreen(state = state, onEvent = onEvent)
    }
}
