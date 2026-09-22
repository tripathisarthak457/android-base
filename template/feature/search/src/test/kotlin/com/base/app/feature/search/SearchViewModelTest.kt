package com.base.app.feature.search

import androidx.lifecycle.SavedStateHandle
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.search.SearchRepository
import com.base.app.data.search.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `typing quickly sends one search for the final query`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, SavedStateHandle())

        listOf("w", "we", "wea", "weather").forEach { viewModel.onEvent(SearchEvent.QueryChanged(it)) }
        advanceUntilIdle()

        assertEquals(listOf("weather"), repository.searched)
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    @Test
    fun `no results is Empty`() = runTest {
        val repository = FakeSearchRepository(results = emptyList())
        val viewModel = SearchViewModel(repository, SavedStateHandle())

        viewModel.onEvent(SearchEvent.QueryChanged("zzz"))
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a query is remembered on submit, not while typing`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, SavedStateHandle())

        viewModel.onEvent(SearchEvent.QueryChanged("cats"))
        advanceUntilIdle()
        assertEquals(emptyList<String>(), repository.recentFlow.value)

        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()
        assertEquals(listOf("cats"), viewModel.state.value.recent)
    }

    @Test
    fun `clearing the query returns to the start without searching`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, SavedStateHandle())

        viewModel.onEvent(SearchEvent.QueryChanged("cats"))
        viewModel.onEvent(SearchEvent.QueryChanged(""))
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.searched)
        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
    }
}

private class FakeSearchRepository(
    private val results: List<SearchResult> = listOf(SearchResult(1, "A title", "A snippet")),
) : SearchRepository {
    val searched = mutableListOf<String>()
    val recentFlow = MutableStateFlow<List<String>>(emptyList())
    override val recent: StateFlow<List<String>> = recentFlow

    override suspend fun search(query: String): AppResult<List<SearchResult>> {
        searched += query
        return AppResult.Success(results)
    }

    override suspend fun result(id: Int): AppResult<SearchResult> = AppResult.Success(results.first())

    override suspend fun remember(query: String) {
        recentFlow.value = listOf(query) + recentFlow.value
    }

    override suspend fun forget(query: String) {
        recentFlow.value = recentFlow.value - query
    }

    override suspend fun clearRecent() {
        recentFlow.value = emptyList()
    }
}
