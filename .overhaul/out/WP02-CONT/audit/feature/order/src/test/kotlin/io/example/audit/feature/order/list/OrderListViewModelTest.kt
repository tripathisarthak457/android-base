package io.example.audit.feature.order.list

import io.example.audit.core.common.LoadState
import io.example.audit.core.common.result.AppError
import io.example.audit.core.common.result.AppResult
import io.example.audit.core.testing.MainDispatcherRule
import io.example.audit.data.order.api.FakeOrderRepository
import io.example.audit.data.order.api.model.OrderItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrderListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        OrderItem(id = "1", title = "One"),
        OrderItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeOrderRepository(AppResult.Success(items))
        val viewModel = OrderListViewModel(repository)

        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeOrderRepository(AppResult.Success(emptyList()))
        val viewModel = OrderListViewModel(repository)

        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeOrderRepository(AppResult.Success(emptyList()))
        val viewModel = OrderListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeOrderRepository(AppResult.Success(items))
        val viewModel = OrderListViewModel(repository)
        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(OrderListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeOrderRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = OrderListViewModel(repository)

        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeOrderRepository(AppResult.Failure(AppError.Offline))
        val viewModel = OrderListViewModel(repository)

        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(OrderListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after a failed first load replace the error`() = runTest {
        val repository = FakeOrderRepository(AppResult.Failure(AppError.Offline))
        val viewModel = OrderListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(OrderListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }
}
