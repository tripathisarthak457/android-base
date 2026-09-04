package com.base.app.feature.sample

import app.cash.turbine.test
import com.base.app.core.common.AppResult
import com.base.app.core.common.mvi.LoadState
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.sample.SampleItem
import com.base.app.data.sample.SampleRepository
import com.base.app.feature.sample.list.SampleListEffect
import com.base.app.feature.sample.list.SampleListEvent
import com.base.app.feature.sample.list.SampleListViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The reference ViewModel test. Every feature's tests are shaped like these.
 *
 * No Robolectric, no instrumentation, no network: the repository is a fake and the dispatcher is
 * a test one, so the whole file runs in milliseconds. That is the practical payoff of injecting
 * dispatchers and putting the repository behind an interface.
 */
class SampleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        SampleItem(1, "Alpha", "first"),
        SampleItem(2, "Beta", "second"),
    )

    @Test
    fun `loads on creation and lands in Success`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)))

        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(emptyList())))

        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed first load surfaces the error and its offline flag`() = runTest {
        val repository = FakeSampleRepository(
            AppResult.Failure(message = "Nope", isOffline = true),
        )
        val viewModel = SampleListViewModel(repository)

        advanceUntilIdle()

        val loadState = viewModel.state.value.loadState
        assertTrue(loadState is LoadState.Error)
        assertTrue((loadState as LoadState.Error).isOffline)
    }

    @Test
    fun `a failed refresh keeps the content already on screen`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository)
        advanceUntilIdle()

        repository.result = AppResult.Failure(message = "Flaky")
        viewModel.onEvent(SampleListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `the query filters without touching the loaded list`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)))
        advanceUntilIdle()

        viewModel.onEvent(SampleListEvent.QueryChanged("alp"))
        advanceUntilIdle()

        assertEquals(listOf(items[0]), viewModel.state.value.visibleItems)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `tapping a row emits a navigation effect`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)))
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(SampleListEvent.ItemClicked(2))
            assertEquals(SampleListEffect.OpenDetail(2), awaitItem())
        }
    }
}

private class FakeSampleRepository(
    var result: AppResult<List<SampleItem>>,
) : SampleRepository {

    override suspend fun items(forceRefresh: Boolean): AppResult<List<SampleItem>> = result

    override suspend fun item(id: Int): AppResult<SampleItem> =
        when (val current = result) {
            is AppResult.Success -> current.data.firstOrNull { it.id == id }
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(message = "Not found", code = 404)

            is AppResult.Failure -> current
        }
}
