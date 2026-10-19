package com.example.wp04.everything.feature.sample.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.wp04.everything.core.common.LoadState
import com.example.wp04.everything.core.common.result.AppError
import com.example.wp04.everything.core.common.result.AppResult
import com.example.wp04.everything.core.testing.MainDispatcherRule
import com.example.wp04.everything.data.sample.api.SampleItem
import com.example.wp04.everything.data.sample.api.SampleRepository
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** The reference ViewModel test. Every feature's tests are shaped like these. */
class SampleListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val items = listOf(
        SampleItem(1, "Alpha", "first"),
        SampleItem(2, "Beta", "second"),
    )

    @Test
    fun `appearing loads and lands in Success`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())

        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.visibleItems)
    }

    @Test
    fun `appearing again does not reload what is already on screen`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository, SavedStateHandle())

        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()
        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(emptyList())), SavedStateHandle())

        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed first load surfaces the error`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Failure(AppError.Offline)), SavedStateHandle())

        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the content already on screen`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository, SavedStateHandle())
        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Timeout)
        viewModel.onEvent(SampleListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `typing filters without touching the loaded list`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())
        viewModel.state.launchIn(backgroundScope)
        viewModel.onEvent(SampleListEvent.Appeared)
        advanceUntilIdle()

        viewModel.query.type("alp")
        advanceUntilIdle()

        assertEquals(listOf(items[0]), viewModel.state.value.visibleItems)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `tapping a row emits a navigation effect`() = runTest {
        val viewModel = SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())

        viewModel.effects.test {
            viewModel.onEvent(SampleListEvent.ItemClicked(2))
            assertEquals(SampleListEffect.OpenDetail(2), awaitItem())
        }
    }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}

private class FakeSampleRepository(
    var result: AppResult<List<SampleItem>>,
) : SampleRepository {
    var loads = 0

    override suspend fun items(forceRefresh: Boolean): AppResult<List<SampleItem>> {
        loads++
        return result
    }

    override suspend fun item(id: Int): AppResult<SampleItem> =
        when (val current = result) {
            is AppResult.Success -> current.value.firstOrNull { it.id == id }
                ?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Http(status = 404))

            is AppResult.Failure -> current
        }
}
