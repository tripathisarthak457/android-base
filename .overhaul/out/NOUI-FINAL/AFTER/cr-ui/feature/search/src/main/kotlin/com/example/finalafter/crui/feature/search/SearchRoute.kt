package com.example.finalafter.crui.feature.search

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crui.core.ui.MviScreen

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
    ) { state, onEvent ->
        SearchScreen(state = state, query = viewModel.query, onEvent = onEvent)
    }
}
