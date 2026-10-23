package com.example.standard.feature.sample.detail

import com.example.standard.core.common.LoadState
import com.example.standard.core.common.result.AppError
import com.example.standard.core.common.result.AppResult
import com.example.standard.core.testing.MainDispatcherRule
import com.example.standard.data.sample.api.SampleRepository
import com.example.standard.data.sample.api.model.SampleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        val viewModel = SampleDetailViewModel(FakeSampleRepository(items), itemId = 4)

        viewModel.onEvent(SampleDetailEvent.Appeared)
        advanceUntilIdle()

        assertEquals(items[3], viewModel.state.value.item)
        assertEquals(listOf(5, 1, 2), viewModel.state.value.related.map { it.id })
    }

    @Test
    fun `a failed load shows the error and no related rows`() = runTest {
        val viewModel = SampleDetailViewModel(FakeSampleRepository(items, failing = true), itemId = 1)

        viewModel.onEvent(SampleDetailEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
        assertEquals(emptyList<SampleItem>(), viewModel.state.value.related)
    }
}

private class FakeSampleRepository(
    private val all: List<SampleItem>,
    private val failing: Boolean = false,
) : SampleRepository {

    override val items: Flow<List<SampleItem>> = MutableStateFlow(all)

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    override fun peek(id: Int): SampleItem? = all.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> =
        if (failing) AppResult.Failure(AppError.Offline) else AppResult.Success(all.first { it.id == id })
}
