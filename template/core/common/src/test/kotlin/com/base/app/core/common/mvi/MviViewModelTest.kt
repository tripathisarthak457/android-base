package com.base.app.core.common.mvi

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The two helpers in [MviViewModel] that are more than a line of plumbing.
 *
 * Both fail silently when broken — a state that quietly does not come back, a stale response that
 * quietly wins — so they are exactly the parts worth pinning down.
 */
class MviViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a later job cancels the one before it under the same key`() = runTest(dispatcher) {
        val viewModel = TestViewModel()

        viewModel.onEvent(TestEvent.Search("ca"))
        viewModel.onEvent(TestEvent.Search("cars"))
        advanceUntilIdleCompat()

        // Both handlers ran, but only the second one's write survives: the first was cancelled
        // mid-flight. Without launchLatest the slow "ca" response lands last and wins.
        assertEquals("cars", viewModel.state.value.results)
    }

    @Test
    fun `what the user typed comes back, and what can be recomputed does not`() =
        runTest(dispatcher) {
            val handle = SavedStateHandle()

            TestViewModel(handle).apply {
                onEvent(TestEvent.Search("stored"))
                advanceUntilIdleCompat()
            }

            // A second ViewModel over the same handle stands in for the process being killed and
            // the screen being recreated.
            val restored = TestViewModel(handle)
            advanceUntilIdleCompat()

            assertEquals("stored", restored.state.value.query)
            assertEquals("", restored.state.value.results)
        }

    private suspend fun advanceUntilIdleCompat() = delay(SETTLE_MILLIS)

    private companion object {
        const val SETTLE_MILLIS = 1_000L
        const val SLOW_MILLIS = 100L
    }

    private data class TestState(
        val query: String = "",
        val results: String = "",
    ) : UiState

    private sealed interface TestEvent : UiEvent {
        data class Search(val query: String) : TestEvent
    }

    private class TestViewModel(
        handle: SavedStateHandle = SavedStateHandle(),
    ) : MviViewModel<TestState, TestEvent, NoEffect>(TestState()) {

        init {
            persistState(
                handle = handle,
                save = { mapOf("query" to it.query) },
                restore = { copy(query = it["query"] as? String ?: query) },
            )
        }

        override suspend fun handleEvent(event: TestEvent) {
            when (event) {
                is TestEvent.Search -> {
                    updateState { copy(query = event.query) }
                    launchLatest("search") {
                        // The shorter query is the slower one, which is the shape of the real
                        // race: "ca" matches more rows than "cars" and the server takes
                        // longer over it, so its answer lands last and wins. Equal delays
                        // would let this test pass without any cancellation at all.
                        delay(SLOW_MILLIS * (LONGEST_QUERY - event.query.length))
                        updateState { copy(results = event.query) }
                    }
                }
            }
        }

        private companion object {
            const val SLOW_MILLIS = 100L
            const val LONGEST_QUERY = 8
        }
    }
}
