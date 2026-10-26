package com.example.cmp.feature.home.list

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.feature.home.api.HomeDetailKey

@Composable
internal fun HomeListRoute(
    navigator: AppNavigator,
    viewModel: HomeListViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is HomeListEffect.OpenDetail -> {
                    navigator.navigate(HomeDetailKey(effect.id))
                }
            }
        },
    ) { state, onEvent ->
        HomeListScreen(state = state, onEvent = onEvent)
    }
}
