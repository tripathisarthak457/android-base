package com.example.cmp.feature.feed

import com.example.cmp.core.testing.MainDispatcherRule
import com.example.cmp.data.feed.api.FakeFeedRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `a tapped post expands, and a second tap collapses it`() {
        val viewModel = FeedViewModel(FakeFeedRepository())

        viewModel.onEvent(FeedEvent.ToggleExpanded(postId = 7))
        assertEquals(setOf(7), viewModel.state.value.expanded)

        viewModel.onEvent(FeedEvent.ToggleExpanded(postId = 7))
        assertEquals(emptySet<Int>(), viewModel.state.value.expanded)
    }

    @Test
    fun `each post expands on its own`() {
        val viewModel = FeedViewModel(FakeFeedRepository())

        viewModel.onEvent(FeedEvent.ToggleExpanded(postId = 1))
        viewModel.onEvent(FeedEvent.ToggleExpanded(postId = 2))
        viewModel.onEvent(FeedEvent.ToggleExpanded(postId = 1))

        assertEquals(setOf(2), viewModel.state.value.expanded)
    }

    // A rotation collects the pages again: they must come from the ViewModel's cache, not from a
    // second pager that starts the feed over from the first page.
    @Test
    fun `a second collector reads the pages already fetched`() = runTest {
        val repository = FakeFeedRepository()
        val viewModel = FeedViewModel(repository)

        viewModel.posts.launchIn(backgroundScope)
        runCurrent()
        viewModel.posts.launchIn(backgroundScope)
        runCurrent()

        assertEquals(1, repository.fetches)
    }
}
