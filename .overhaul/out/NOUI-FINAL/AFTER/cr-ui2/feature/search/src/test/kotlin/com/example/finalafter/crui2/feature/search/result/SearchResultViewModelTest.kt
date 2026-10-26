package com.example.finalafter.crui2.feature.search.result

import app.cash.turbine.test
import com.example.finalafter.crui2.core.common.LoadState
import com.example.finalafter.crui2.core.testing.MainDispatcherRule
import com.example.finalafter.crui2.data.search.api.FakeSearchRepository
import com.example.finalafter.crui2.data.search.api.model.SearchResult
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SearchResultViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val result = SearchResult(3, "Weather", "Rain by noon")

    @Test
    fun `the result loads once the screen collects the state, and not before`() = runTest {
        val viewModel = SearchResultViewModel(FakeSearchRepository(results = listOf(result)), id = 3)
        advanceUntilIdle()
        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
        assertNull(viewModel.state.value.result)

        viewModel.state.launchIn(backgroundScope)
        runCurrent()
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(result, viewModel.state.value.result)
    }

    @Test
    fun `back asks the route to go back`() = runTest {
        val viewModel = SearchResultViewModel(FakeSearchRepository(results = listOf(result)), id = 3)

        viewModel.effects.test {
            viewModel.onEvent(SearchResultEvent.BackClicked)
            assertEquals(SearchResultEffect.NavigateBack, awaitItem())
        }
    }
}
