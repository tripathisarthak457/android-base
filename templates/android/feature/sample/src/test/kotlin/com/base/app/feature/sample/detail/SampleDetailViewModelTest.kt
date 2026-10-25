package com.base.app.feature.sample.detail

import com.base.app.core.common.LoadState
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.sample.api.FakeSampleRepository
import com.base.app.data.sample.api.model.SampleItem
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SampleDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = (1..5).map { SampleItem(it, "Item $it", "Body $it") }

    @Test
    fun `related rows are the items that follow, wrapping at the end`() = runTest {
        val viewModel = SampleDetailViewModel(FakeSampleRepository(AppResult.Success(items)), itemId = 4)

        viewModel.onEvent(SampleDetailEvent.Appeared)
        advanceUntilIdle()

        assertEquals(items[3], viewModel.state.value.item)
        assertEquals(listOf(5, 1, 2), viewModel.state.value.related.map { it.id })
    }

    @Test
    fun `a failed load shows the error and no related rows`() = runTest {
        val viewModel = SampleDetailViewModel(FakeSampleRepository(AppResult.Failure(AppError.Offline)), itemId = 1)

        viewModel.onEvent(SampleDetailEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
        assertEquals(emptyList<SampleItem>(), viewModel.state.value.related)
    }
}
