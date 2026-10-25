package com.base.app.feature.feed

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.base.app.core.ui.MviScreen
// <opt:ui_components>
import androidx.paging.compose.collectAsLazyPagingItems
// </opt:ui_components>
// <opt-else:ui_components>
//~ import com.base.app.core.ui.PlaceholderScreen
// </opt-else:ui_components>

@Composable
internal fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    // <opt:ui_components>
    val posts = viewModel.posts.collectAsLazyPagingItems()
    MviScreen(viewModel = viewModel, onEffect = {}) { state, onEvent ->
        FeedScreen(state = state, posts = posts, onEvent = onEvent)
    }
    // </opt:ui_components>
    // <opt-else:ui_components>
    //~ MviScreen(viewModel = viewModel, onEffect = {}) { _, _ ->
    //~     PlaceholderScreen(title = R.string.feed_title)
    //~ }
    // </opt-else:ui_components>
}
