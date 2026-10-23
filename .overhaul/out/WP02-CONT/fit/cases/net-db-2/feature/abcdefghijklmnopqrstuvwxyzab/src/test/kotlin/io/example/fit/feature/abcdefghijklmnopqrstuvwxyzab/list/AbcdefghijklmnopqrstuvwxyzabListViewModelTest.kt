package io.example.fit.feature.abcdefghijklmnopqrstuvwxyzab.list

import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.testing.MainDispatcherRule
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.FakeAbcdefghijklmnopqrstuvwxyzabRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AbcdefghijklmnopqrstuvwxyzabListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        AbcdefghijklmnopqrstuvwxyzabItem(id = "1", title = "One"),
        AbcdefghijklmnopqrstuvwxyzabItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository =
            FakeAbcdefghijklmnopqrstuvwxyzabRepository(AppResult.Failure(AppError.Offline))
        val viewModel = AbcdefghijklmnopqrstuvwxyzabListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyzabListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
