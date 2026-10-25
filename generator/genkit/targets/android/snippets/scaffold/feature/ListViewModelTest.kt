package __PKG__.feature.__NAME__.list

import __PKG__.core.common.LoadState
import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.core.testing.MainDispatcherRule
import __PKG__.data.__NAME__.api.Fake__P__Repository
import __PKG__.data.__NAME__.api.__P__Item
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class __P__ListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        __P__Item(id = "1", title = "One"),
        __P__Item(id = "2", title = "Two"),
    )

    @Test
    fun `nothing loads until the screen collects the state, then it loads once`() = runTest {
        val repository = Fake__P__Repository(AppResult.Success(items))
        val viewModel = __P__ListViewModel(repository)
        advanceUntilIdle()
        assertEquals(0, repository.loads)

        shown(viewModel)
        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty answer is Empty, not Success with no rows`() = runTest {
__FIT__
        val repository = Fake__P__Repository(AppResult.Success(emptyList()))
__ELSE__
        val repository =
            Fake__P__Repository(AppResult.Success(emptyList()))
__END_FIT__
        val viewModel = __P__ListViewModel(repository)

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the rows on screen`() = runTest {
        val repository = Fake__P__Repository(AppResult.Success(items))
        val viewModel = __P__ListViewModel(repository)
        shown(viewModel)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Offline)
        viewModel.onEvent(__P__ListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `a failed first load is an error the screen can retry`() = runTest {
__FIT__
        val repository = Fake__P__Repository(AppResult.Failure(AppError.Offline))
__ELSE__
        val repository =
            Fake__P__Repository(AppResult.Failure(AppError.Offline))
__END_FIT__
        val viewModel = __P__ListViewModel(repository)

        shown(viewModel)
        advanceUntilIdle()
        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)

        repository.result = AppResult.Success(items)
        viewModel.onEvent(__P__ListEvent.Retry)
        advanceUntilIdle()
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    /** The screen collecting the state, as its route does once it is started. */
    private fun TestScope.shown(viewModel: __P__ListViewModel) {
        viewModel.state.launchIn(backgroundScope)
        runCurrent()
    }
}
