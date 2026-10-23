package io.example.fitcnet.feature.abcdefghijklmnopqrstuvw.list

import io.example.fitcnet.core.common.LoadState
import io.example.fitcnet.core.common.result.AppError
import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.testing.MainDispatcherRule
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.api.FakeAbcdefghijklmnopqrstuvwRepository
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AbcdefghijklmnopqrstuvwListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        AbcdefghijklmnopqrstuvwItem(id = "1", title = "One"),
        AbcdefghijklmnopqrstuvwItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwRepository(AppResult.Failure(AppError.Offline))
        val viewModel = AbcdefghijklmnopqrstuvwListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
