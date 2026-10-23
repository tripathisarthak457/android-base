package com.example.lean.feature.home.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.lean.core.navigation.AppNavigator
import com.example.lean.core.ui.MviScreen
import com.example.lean.feature.home.api.HomeDetailKey

@Composable
internal fun HomeListRoute(
    navigator: AppNavigator,
    viewModel: HomeListViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.onEvent(HomeListEvent.Appeared) }

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
