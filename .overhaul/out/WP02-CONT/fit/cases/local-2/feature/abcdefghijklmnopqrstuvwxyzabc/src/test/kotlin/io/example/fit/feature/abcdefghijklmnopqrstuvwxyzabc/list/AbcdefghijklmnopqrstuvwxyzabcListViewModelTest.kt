package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzabc.list

import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.testing.MainDispatcherRule
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.FakeAbcdefghijklmnopqrstuvwxyzabcRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.model.AbcdefghijklmnopqrstuvwxyzabcItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AbcdefghijklmnopqrstuvwxyzabcListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        AbcdefghijklmnopqrstuvwxyzabcItem(id = "1", title = "One"),
        AbcdefghijklmnopqrstuvwxyzabcItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository =
            FakeAbcdefghijklmnopqrstuvwxyzabcRepository(AppResult.Failure(AppError.Offline))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabcListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabcListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
