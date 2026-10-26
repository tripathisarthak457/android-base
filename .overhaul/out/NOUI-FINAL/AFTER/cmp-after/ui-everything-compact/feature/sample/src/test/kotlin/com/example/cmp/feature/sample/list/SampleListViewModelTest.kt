package com.example.cmp.feature.sample.list

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.cmp.core.common.LoadState
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.testing.MainDispatcherRule
import com.example.cmp.data.sample.api.FakeSampleRepository
import com.example.cmp.data.sample.api.model.SampleItem
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
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
    fun `the first look at the screen loads and lands in Success`() = runTest {
        val viewModel =
            SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.visibleItems)
    }

    // A ViewModel is also built for a restored back stack entry or a screen prepared ahead of time;
    // neither may fetch before the screen is shown.
    @Test
    fun `nothing loads until the screen collects the state`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository, SavedStateHandle())

        advanceUntilIdle()

        assertEquals(0, repository.loads)
        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
    }

    @Test
    fun `a second collector, or the screen coming back later, does not load again`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository, SavedStateHandle())

        val screen = shown(viewModel)
        val rotated = shown(viewModel)
        advanceUntilIdle()
        screen.cancel()
        rotated.cancel()
        // Past the grace period, as when the person returns from another app.
        advanceTimeBy(6.seconds)
        shown(viewModel)
        advanceUntilIdle()

        assertEquals(1, repository.loads)
    }

    @Test
    fun `an empty response is Empty, not Success with no rows`() = runTest {
        val viewModel =
            SampleListViewModel(
                FakeSampleRepository(AppResult.Success(emptyList())),
                SavedStateHandle(),
            )

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a failed first load surfaces the error`() = runTest {
        val viewModel =
            SampleListViewModel(
                FakeSampleRepository(AppResult.Failure(AppError.Offline)),
                SavedStateHandle(),
            )

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Error(AppError.Offline), viewModel.state.value.loadState)
    }

    @Test
    fun `a failed refresh keeps the content already on screen`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        val viewModel = SampleListViewModel(repository, SavedStateHandle())
        shown(viewModel)
        advanceUntilIdle()

        repository.result = AppResult.Failure(AppError.Timeout)
        viewModel.onEvent(SampleListEvent.Refresh)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `rows already stored are shown when the refresh at launch fails`() = runTest {
        val repository = FakeSampleRepository(AppResult.Success(items))
        repository.refresh()
        repository.result = AppResult.Failure(AppError.Offline)
        val viewModel = SampleListViewModel(repository, SavedStateHandle())

        shown(viewModel)
        advanceUntilIdle()

        assertEquals(LoadState.Success, viewModel.state.value.loadState)
        assertEquals(items, viewModel.state.value.visibleItems)
    }

    @Test
    fun `typing filters without touching the loaded list`() = runTest {
        val viewModel =
            SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())
        shown(viewModel)
        advanceUntilIdle()

        viewModel.query.type("alp")
        advanceUntilIdle()

        assertEquals(listOf(items[0]), viewModel.state.value.visibleItems)
        assertEquals(items, viewModel.state.value.items)
    }

    @Test
    fun `tapping a row emits a navigation effect`() = runTest {
        val viewModel =
            SampleListViewModel(FakeSampleRepository(AppResult.Success(items)), SavedStateHandle())

        viewModel.effects.test {
            viewModel.onEvent(SampleListEvent.ItemClicked(2))
            assertEquals(SampleListEffect.OpenDetail(2), awaitItem())
        }
    }

    /**
     * The screen collecting the state, as its route does once it is started. runCurrent, so the
     * collector has subscribed before the test goes on: advanceUntilIdle returns while only
     * background work is queued.
     */
    private fun TestScope.shown(viewModel: SampleListViewModel): Job =
        viewModel.state.launchIn(backgroundScope).also { runCurrent() }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}
