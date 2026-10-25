package com.base.app.feature.search

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.SavedStateHandle
import com.base.app.core.common.LoadState
import com.base.app.core.testing.MainDispatcherRule
import com.base.app.data.search.api.FakeSearchRepository
import com.base.app.data.search.api.SearchRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `typing quickly sends one search for the final query`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = shown(repository)

        listOf("w", "we", "wea", "weather").forEach {
            viewModel.query.type(it)
            runCurrent()
        }
        advanceUntilIdle()

        assertEquals(listOf("weather"), repository.searched)
        assertEquals(LoadState.Success, viewModel.state.value.loadState)
    }

    // A query restored after process death is searched again, but only once the screen is shown.
    @Test
    fun `typing is searched from the first look at the screen, not before`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = SearchViewModel(repository, SavedStateHandle())
        viewModel.query.type("cats")
        advanceUntilIdle()
        assertEquals(emptyList<String>(), repository.searched)

        viewModel.state.launchIn(backgroundScope)
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("cats"), repository.searched)
    }

    @Test
    fun `no results is Empty`() = runTest {
        val viewModel = shown(FakeSearchRepository(results = emptyList()))

        viewModel.query.type("zzz")
        advanceUntilIdle()

        assertEquals(LoadState.Empty, viewModel.state.value.loadState)
    }

    @Test
    fun `a query is remembered on submit, not while typing`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = shown(repository)

        viewModel.query.type("cats")
        advanceUntilIdle()
        assertEquals(emptyList<String>(), repository.recent.value)

        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()
        assertEquals(listOf("cats"), viewModel.state.value.recent)
    }

    @Test
    fun `a tapped recent search runs at once and only once`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = shown(repository)

        viewModel.onEvent(SearchEvent.RecentClicked("cats"))
        Snapshot.sendApplyNotifications()
        advanceUntilIdle()

        assertEquals(listOf("cats"), repository.searched)
        assertEquals("cats", viewModel.query.text.toString())
    }

    @Test
    fun `clearing the query returns to the start without searching`() = runTest {
        val repository = FakeSearchRepository()
        val viewModel = shown(repository)

        viewModel.query.type("cats")
        runCurrent()
        viewModel.query.type("")
        advanceUntilIdle()

        assertEquals(emptyList<String>(), repository.searched)
        assertEquals(LoadState.Idle, viewModel.state.value.loadState)
    }

    @Test
    fun `two removals in quick succession both reach the store`() = runTest {
        val repository = FakeSearchRepository(recent = listOf("cats", "dogs", "owls"))
        val disk = CompletableDeferred<Unit>()
        repository.writes = disk
        val viewModel = shown(repository)

        viewModel.onEvent(SearchEvent.RecentRemoved("cats"))
        runCurrent()
        viewModel.onEvent(SearchEvent.RecentRemoved("dogs"))
        runCurrent()
        disk.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("owls"), repository.recent.value)
    }

    @Test
    fun `a search submitted while another is being remembered is remembered too`() = runTest {
        val repository = FakeSearchRepository()
        val disk = CompletableDeferred<Unit>()
        repository.writes = disk
        val viewModel = shown(repository)

        viewModel.query.type("cats")
        viewModel.onEvent(SearchEvent.Submit)
        runCurrent()
        viewModel.onEvent(SearchEvent.RecentClicked("dogs"))
        runCurrent()
        disk.complete(Unit)
        advanceUntilIdle()

        assertEquals(setOf("cats", "dogs"), repository.recent.value.toSet())
    }

    /** A search screen on show: its route collects the state once it is started. */
    private fun TestScope.shown(repository: SearchRepository): SearchViewModel =
        SearchViewModel(repository, SavedStateHandle()).also {
            it.state.launchIn(backgroundScope)
            runCurrent()
        }
}

/** What a keyboard does to the field, plus the frame tick that tells snapshot observers about it. */
private fun TextFieldState.type(text: String) {
    edit { replace(0, length, text) }
    Snapshot.sendApplyNotifications()
}
