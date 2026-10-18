package com.example.wp04.base.feature.wallet

import com.example.wp04.base.core.common.AppResult
import com.example.wp04.base.core.common.mvi.LoadState
import com.example.wp04.base.core.testing.MainDispatcherRule
import com.example.wp04.base.data.wallet.WalletItem
import com.example.wp04.base.data.wallet.WalletRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WalletViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `loads on creation`() = runTest {
        val items = listOf(WalletItem(1, "One"))
        val viewModel = WalletViewModel(FakeWalletRepository(AppResult.Success(items)))

        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {
        val viewModel = WalletViewModel(FakeWalletRepository(AppResult.Success(emptyList())))

        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failure surfaces as an error state`() = runTest {
        val viewModel = WalletViewModel(
            FakeWalletRepository(AppResult.Failure(message = "Nope")),
        )

        advanceUntilIdle()

        assertTrue(viewModel.state.value.loadState is LoadState.Error)
    }
}

private class FakeWalletRepository(
    private val result: AppResult<List<WalletItem>>,
) : WalletRepository {
    override suspend fun items(): AppResult<List<WalletItem>> = result
}
