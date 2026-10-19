package com.example.cicheck.feature.orders

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cicheck.core.navigation.AppNavigator
import com.example.cicheck.core.ui.MviScreen

/** Connects the ViewModel, the screen and navigation, keeping the screen previewable. */
@Composable
fun OrdersListRoute(
    navigator: AppNavigator,
    viewModel: OrdersViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is OrdersEffect.OpenDetail -> navigator.navigate(OrdersDetailKey(effect.id))
            }
        },
    ) { state, onEvent ->
        OrdersScreen(state = state, onEvent = onEvent)
    }
}
