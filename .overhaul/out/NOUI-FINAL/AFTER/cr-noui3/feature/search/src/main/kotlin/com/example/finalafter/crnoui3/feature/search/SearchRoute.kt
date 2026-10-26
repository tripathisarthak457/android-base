package com.example.finalafter.crnoui3.feature.search

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui3.core.ui.MviScreen
import com.example.finalafter.crnoui3.core.ui.PlaceholderScreen

@Composable
internal fun SearchRoute(
    onOpenResult: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is SearchEffect.OpenResult -> onOpenResult(effect.id)
            }
        },
    ) { _, _ ->
        PlaceholderScreen(title = R.string.search_title)
    }
}
