package __PKG__.feature.__NAME__.list

import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.core.testing.MainDispatcherRule
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.data.__NAME__.api.__P__Repository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class __P__ListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(__P__Item(id = "1", title = "One"), __P__Item(id = "2", title = "Two"))

    @Test
    fun `appearing loads once and lands in Success`() = runTest {
        val repository = Fake__P__Repository(AppResult.Success(items))
        val viewModel = __P__ListViewModel(repository)

        viewModel.onEvent(__P__ListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(__P__ListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
        val viewModel = __P__ListViewModel(Fake__P__Repository(AppResult.Success(emptyList())))

        viewModel.onEvent(__P__ListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = Fake__P__Repository(AppResult.Success(items))
        val viewModel = __P__ListViewModel(repository)
        viewModel.onEvent(__P__ListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(__P__ListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
        val repository = Fake__P__Repository(AppResult.Failure(AppError.Offline))
        val viewModel = __P__ListViewModel(repository)

        viewModel.onEvent(__P__ListEvent.Appeared)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(__P__ListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }
}

private class Fake__P__Repository(var result: AppResult<List<__P__Item>>) : __P__Repository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<__P__Item> =
        when (val current = result) {
            is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
            is AppResult.Failure -> current
        }
}
