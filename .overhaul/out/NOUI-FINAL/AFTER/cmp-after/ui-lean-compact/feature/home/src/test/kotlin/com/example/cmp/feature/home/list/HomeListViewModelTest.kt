package com.example.cmp.feature.home.list

import com.example.cmp.core.common.LoadState
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.testing.MainDispatcherRule
import com.example.cmp.data.home.api.FakeHomeRepository
import com.example.cmp.data.home.api.HomeItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        HomeItem(id = "1", title = "One"),
        HomeItem(id = "2", title = "Two"),
    )

    @Test
    fun `nothing loads until the screen collects the state, then it loads once`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(items))
        val viewModel = HomeListViewModel(repository)
        advanceUntilIdle()
        assertEquals(0, repository.loads)

        shown(viewModel)
        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(emptyList()))
        val viewModel = HomeListViewModel(repository)

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(items))
        val viewModel = HomeListViewModel(repository)
        shown(viewModel)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(HomeListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeHomeRepository(AppResult.Failure(AppError.Offline))
        val viewModel = HomeListViewModel(repository)

        shown(viewModel)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(HomeListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    /** The screen collecting the state, as its route does once it is started. */
    private fun TestScope.shown(viewModel: HomeListViewModel) {
        viewModel.state.launchIn(backgroundScope)
        runCurrent()
    }
}
