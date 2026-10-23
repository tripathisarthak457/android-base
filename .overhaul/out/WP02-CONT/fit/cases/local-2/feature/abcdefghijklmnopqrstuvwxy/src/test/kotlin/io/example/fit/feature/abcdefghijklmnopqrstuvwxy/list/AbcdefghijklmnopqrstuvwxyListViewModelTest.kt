package io.example.fit.feature.abcdefghijklmnopqrstuvwxy.list

import io.example.fit.core.common.LoadState
import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.core.testing.MainDispatcherRule
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.FakeAbcdefghijklmnopqrstuvwxyRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AbcdefghijklmnopqrstuvwxyListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        AbcdefghijklmnopqrstuvwxyItem(id = "1", title = "One"),
        AbcdefghijklmnopqrstuvwxyItem(id = "2", title = "Two"),
    )

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `rows written after an empty answer replace the empty state`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Success(emptyList()))
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()

        repository.write(items)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Success(items))
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the first load fails`() = runTest {
        val repository = FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository =
            FakeAbcdefghijklmnopqrstuvwxyRepository(AppResult.Failure(AppError.Offline))
        val viewModel = AbcdefghijklmnopqrstuvwxyListViewModel(repository)

        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(AbcdefghijklmnopqrstuvwxyListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}
