package com.example.none.feature.home.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.none.core.navigation.AppNavigator
import com.example.none.core.ui.MviScreen
import com.example.none.feature.home.api.HomeDetailKey

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
