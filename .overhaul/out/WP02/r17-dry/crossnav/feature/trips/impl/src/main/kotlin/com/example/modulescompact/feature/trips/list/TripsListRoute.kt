package com.example.modulescompact.feature.trips.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulescompact.core.navigation.AppNavigator
import com.example.modulescompact.core.ui.MviScreen
import com.example.modulescompact.feature.orders.api.OrderDetailKey

@Composable
internal fun TripsListRoute(
    navigator: AppNavigator,
    viewModel: TripsListViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(TripsListEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is TripsListEffect.OpenDetail -> {
                    navigator.navigate(OrderDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        TripsListScreen(state = state, onEvent = onEvent)
    }
}
