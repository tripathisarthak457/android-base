package com.base.app.feature.search

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.ui.MviScreen
// <opt:!ui_components>import com.base.app.core.ui.PlaceholderScreen

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
        // <opt:ui_components>
    ) { state, onEvent ->
        SearchScreen(state = state, query = viewModel.query, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ ) { _, _ ->
    //~     PlaceholderScreen(title = R.string.search_title)
    //~ }
    // </opt-else:ui_components>
}
