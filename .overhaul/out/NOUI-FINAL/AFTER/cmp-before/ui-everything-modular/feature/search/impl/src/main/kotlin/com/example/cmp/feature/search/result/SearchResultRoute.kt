package com.example.cmp.feature.search.result

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.ui.MviScreen

@Composable
internal fun SearchResultRoute(
    id: Int,
    navigator: AppNavigator,
    viewModel: SearchResultViewModel = hiltViewModel<SearchResultViewModel, SearchResultViewModel.Factory>(
        creationCallback = { factory -> factory.create(id) },
    ),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SearchResultEffect.NavigateBack -> navigator.navigateUp()
            }
        },
    ) { state, onEvent ->
        SearchResultScreen(state = state, onEvent = onEvent)
    }
}
