package com.example.modulesmodular.feature.trips.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.modulesmodular.core.navigation.AppNavigator
import com.example.modulesmodular.core.ui.MviScreen

@Composable
internal fun TripsDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: TripsDetailViewModel =
        hiltViewModel<TripsDetailViewModel, TripsDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(TripsDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                TripsDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        TripsDetailScreen(state = state, onEvent = onEvent)
    }
}
