package com.example.modulesmodular.feature.trips.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.ui.MviScreen
import com.example.modulesmodular.feature.trips.api.TripsDetailKey

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
                    navigator.navigate(TripsDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        TripsListScreen(state = state, onEvent = onEvent)
    }
}
