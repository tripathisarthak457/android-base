package io.example.fitcnetdb.feature.abcdefghijklmnopqrs.list

import io.example.fitcnetdb.core.common.LoadState
import io.example.fitcnetdb.core.common.result.AppError
import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.testing.MainDispatcherRule
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.FakeAbcdefghijklmnopqrsRepository
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AbcdefghijklmnopqrsListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        AbcdefghijklmnopqrsItem(id = "1", title = "One"),
        AbcdefghijklmnopqrsItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)
        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = FakeAbcdefghijklmnopqrsRepository(AppResult.Failure(AppError.Offline))
        val viewModel = AbcdefghijklmnopqrsListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(AbcdefghijklmnopqrsListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
