package com.example.finalafter.crui2.feature.feed

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.finalafter.crui2.core.ui.MviScreen

@Composable
internal fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    val posts = viewModel.posts.collectAsLazyPagingItems()
    MviScreen(viewModel = viewModel, onEffect = {}) { state, onEvent ->
        FeedScreen(state = state, posts = posts, onEvent = onEvent)
    }
}
