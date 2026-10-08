package com.base.app.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppResult
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.search.api.SearchRepository
import com.base.app.data.search.api.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
        val viewModel = appeared(repository)

        listOf("w", "we", "wea", "weather").forEach {
            viewModel.query.type(it)
            runCurrent()
        }
        advanceUntilIdle()

        assertEquals(listOf("weather"), repository.searched)
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    @Test
    fun `no results is Empty`() = runTest {
        val viewModel = appeared(FakeSearchRepository(results = emptyList()))

        viewModel.query.type("zzz")
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a query is remembered on submit, not while typing`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = appeared(repository)

        viewModel.query.type("cats")
        advanceUntilIdle()
        assertEquals(emptyList<String>(), repository.recentFlow.value)

        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()
        assertEquals(listOf("cats"), viewModel.state.value.recent)
    }

    @Test
    fun `a tapped recent search runs at once and only once`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = appeared(repository)

        viewModel.onEvent(SearchEvent.RecentClicked("cats"))
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("cats"), repository.searched)
        assertEquals("cats", viewModel.query.text.toString())
    }

    @Test
    fun `clearing the query returns to the start without searching`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = appeared(repository)

        viewModel.query.type("cats")
        runCurrent()
        viewModel.query.type("")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.searched)
        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
    }

    private fun TestScope.appeared(repository: SearchRepository): SearchViewModel =
        SearchViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            it.onEvent(SearchEvent.Appeared)
            runCurrent()
        }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
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
