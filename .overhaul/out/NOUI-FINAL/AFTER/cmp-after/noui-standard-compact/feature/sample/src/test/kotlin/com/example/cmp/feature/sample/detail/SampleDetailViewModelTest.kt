package com.example.cmp.feature.sample.detail

import com.example.cmp.core.common.LoadState
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.testing.MainDispatcherRule
import com.example.cmp.data.sample.api.FakeSampleRepository
import com.example.cmp.data.sample.api.model.SampleItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SampleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = (1..5).map { SampleItem(it, "Item $it", "Body $it") }

    @Test
    fun `nothing loads until the screen collects the state`() = runTest {
        val viewModel =
            SampleDetailViewModel(FakeSampleRepository(AppResult.Success(items)), itemId = 4)

        advanceUntilIdle()

        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
        assertNull(viewModel.state.value.item)
    }

    @Test
    fun `related rows are the items that follow, wrapping at the end`() = runTest {
        val viewModel =
            SampleDetailViewModel(FakeSampleRepository(AppResult.Success(items)), itemId = 4)

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(items[3], viewModel.state.value.item)
        assertEquals(listOf(5, 1, 2), viewModel.state.value.related.map { it.id })
    }

    @Test
    fun `a failed load shows the error and no related rows`() = runTest {
        val viewModel =
            SampleDetailViewModel(
                FakeSampleRepository(AppResult.Failure(AppError.Offline)),
                itemId = 1,
            )

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
        assertEquals(emptyList<SampleItem>(), viewModel.state.value.related)
    }

    @Test
    fun `retry after a failure loads the item`() = runTest {
        val repository = FakeSampleRepository(AppResult.Failure(AppError.Offline))
        val viewModel = SampleDetailViewModel(repository, itemId = 2)
        shown(viewModel)
        advanceUntilIdle()

        repository.result = AppResult.Success(items)
        viewModel.onEvent(SampleDetailEvent.Retry)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items[1], viewModel.state.value.item)
    }

    /** The screen collecting the state, as its route does once it is started. */
    private fun TestScope.shown(viewModel: SampleDetailViewModel) {
        viewModel.state.launchIn(backgroundScope)
        runCurrent()
    }
}
