package com.example.finalafter.crnoui3.feature.feed

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.finalafter.crnoui3.core.ui.MviScreen
import com.example.finalafter.crnoui3.core.ui.PlaceholderScreen

@Composable
internal fun FeedRoute(viewModel: FeedViewModel = hiltViewModel()) {
    MviScreen(viewModel = viewModel, onEffect = {}) { _, _ ->
        PlaceholderScreen(title = R.string.feed_title)
    }
}
