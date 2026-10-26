package com.example.cmp.feature.home.detail

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen

@Composable
internal fun HomeDetailRoute(
    id: String,
    navigator: AppNavigator,
    viewModel: HomeDetailViewModel =
        hiltViewModel<HomeDetailViewModel, HomeDetailViewModel.Factory>(
            creationCallback = { factory -> factory.create(id) },
        ),
) {
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
