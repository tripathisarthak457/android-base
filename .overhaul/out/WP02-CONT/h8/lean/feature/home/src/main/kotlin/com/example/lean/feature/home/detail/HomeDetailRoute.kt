package com.example.lean.feature.home.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lean.core.navigation.AppNavigator
import com.example.lean.core.ui.MviScreen

@Composable
internal fun HomeDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: HomeDetailViewModel =
        hiltViewModel<HomeDetailViewModel, HomeDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(HomeDetailEvent.Appeared) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                HomeDetailEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        HomeDetailScreen(state = state, onEvent = onEvent)
    }
}
