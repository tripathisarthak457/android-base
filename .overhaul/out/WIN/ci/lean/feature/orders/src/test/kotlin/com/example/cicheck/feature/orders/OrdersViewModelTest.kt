package com.example.cicheck.feature.orders

import com.example.cicheck.core.common.LoadState
import com.example.cicheck.core.common.result.AppError
import com.example.cicheck.core.common.result.AppResult
import com.example.cicheck.core.testing.MainDispatcherRule
import com.example.cicheck.data.orders.OrdersItem
import com.example.cicheck.data.orders.OrdersRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OrdersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads on creation`() = runTest {
        val items = listOf(OrdersItem(1, "One"))
        val viewModel = OrdersViewModel(FakeOrdersRepository(AppResult.Success(items)))

        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {
        val viewModel = OrdersViewModel(FakeOrdersRepository(AppResult.Success(emptyList())))

        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failure surfaces as an error state`() = runTest {
        val viewModel = OrdersViewModel(
            FakeOrdersRepository(AppResult.Failure(AppError.Offline)),
        )

        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
    }
}

private class FakeOrdersRepository(
    private val result: AppResult<List<OrdersItem>>,
) : OrdersRepository {
    override suspend fun items(): AppResult<List<OrdersItem>> = result
}
