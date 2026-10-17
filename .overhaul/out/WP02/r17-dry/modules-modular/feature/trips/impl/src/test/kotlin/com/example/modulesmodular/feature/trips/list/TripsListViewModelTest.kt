package com.example.modulesmodular.feature.trips.list

import com.example.modulesmodular.core.common.LoadState
import com.example.modulesmodular.core.common.result.AppError
import com.example.modulesmodular.core.common.result.AppResult
import com.example.modulesmodular.core.testing.MainDispatcherRule
import com.example.modulesmodular.data.trips.api.FakeTripsRepository
import com.example.modulesmodular.data.trips.api.TripsItem
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TripsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        TripsItem(id = "1", title = "One"),
        TripsItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeTripsRepository(AppResult.Success(items))
        val viewModel = TripsListViewModel(repository)

        viewModel.onEvent(TripsListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(TripsListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeTripsRepository(AppResult.Success(emptyList()))
        val viewModel = TripsListViewModel(repository)

        viewModel.onEvent(TripsListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeTripsRepository(AppResult.Success(items))
        val viewModel = TripsListViewModel(repository)
        viewModel.onEvent(TripsListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(TripsListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeTripsRepository(AppResult.Failure(AppError.Offline))
        val viewModel = TripsListViewModel(repository)

        viewModel.onEvent(TripsListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(TripsListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
