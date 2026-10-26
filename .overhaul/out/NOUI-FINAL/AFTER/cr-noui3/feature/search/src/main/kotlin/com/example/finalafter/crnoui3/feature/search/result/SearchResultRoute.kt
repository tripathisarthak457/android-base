package com.example.finalafter.crnoui3.feature.search.result

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui3.core.navigation.AppNavigator
import com.example.finalafter.crnoui3.core.ui.MviScreen
import com.example.finalafter.crnoui3.core.ui.PlaceholderScreen
import com.example.finalafter.crnoui3.feature.search.R

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
    ) { _, _ ->
        PlaceholderScreen(title = R.string.search_title)
    }
}
