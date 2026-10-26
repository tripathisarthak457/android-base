package com.example.cmp.feature.feed

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cmp.core.ui.MviScreen
import com.example.cmp.core.ui.PlaceholderScreen

@Composable
internal fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel, onEffect = {}) { _, _ ->
        PlaceholderScreen(title = R.string.feed_title)
    }
}
