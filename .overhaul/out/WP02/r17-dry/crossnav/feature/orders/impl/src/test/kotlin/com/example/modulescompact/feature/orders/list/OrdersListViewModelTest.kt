package com.example.modulescompact.feature.orders.list

import com.example.modulescompact.core.common.LoadState
import com.example.modulescompact.core.common.result.AppError
import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.testing.MainDispatcherRule
import com.example.modulescompact.data.orders.api.FakeOrdersRepository
import com.example.modulescompact.data.orders.api.OrdersItem
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrdersListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        OrdersItem(id = "1", title = "One"),
        OrdersItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeOrdersRepository(AppResult.Success(items))
        val viewModel = OrdersListViewModel(repository)

        viewModel.onEvent(OrdersListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(OrdersListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeOrdersRepository(AppResult.Success(emptyList()))
        val viewModel = OrdersListViewModel(repository)

        viewModel.onEvent(OrdersListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeOrdersRepository(AppResult.Success(items))
        val viewModel = OrdersListViewModel(repository)
        viewModel.onEvent(OrdersListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(OrdersListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeOrdersRepository(AppResult.Failure(AppError.Offline))
        val viewModel = OrdersListViewModel(repository)

        viewModel.onEvent(OrdersListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(OrdersListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
