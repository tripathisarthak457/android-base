package com.example.videoalone.feature.home.list

import com.example.videoalone.core.common.LoadState
import com.example.videoalone.core.common.result.AppError
import com.example.videoalone.core.common.result.AppResult
import com.example.videoalone.core.testing.MainDispatcherRule
import com.example.videoalone.data.home.api.FakeHomeRepository
import com.example.videoalone.data.home.api.HomeItem
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(items))
        val viewModel = HomeListViewModel(repository)

        viewModel.onEvent(HomeListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(HomeListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(emptyList()))
        val viewModel = HomeListViewModel(repository)

        viewModel.onEvent(HomeListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeHomeRepository(AppResult.Success(items))
        val viewModel = HomeListViewModel(repository)
        viewModel.onEvent(HomeListEvent.Appeared)
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

        viewModel.onEvent(HomeListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(HomeListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
