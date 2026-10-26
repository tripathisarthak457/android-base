package com.base.app.core.mvi

import app.cash.turbine.test
import com.base.app.core.common.result.AppError
import com.base.app.core.testing.MainDispatcherRule
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MviViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `back is handled while a load is still in flight`() = runTest {
        val answer = CompletableDeferred<String>()
        val viewModel = TestViewModel(load = { answer.await() })

        viewModel.effects.test {
            viewModel.onEvent(TestEvent.Load)
            runCurrent()
            viewModel.onEvent(TestEvent.Back)

            assertEquals(TestEffect.NavigateBack, awaitItem())
        }
        answer.complete("late")
    }

    @Test
    fun `a second exclusive run is ignored while the first is active`() = runTest {
        val answer = CompletableDeferred<String>()
        var submits = 0
        val viewModel = TestViewModel(
            submit = {
                submits++
                answer.await()
            },
        )

        viewModel.onEvent(TestEvent.Submit)
        viewModel.onEvent(TestEvent.Submit)
        runCurrent()
        answer.complete("done")
        advanceUntilIdle()

        assertEquals(1, submits)
        assertEquals("done", viewModel.state.value.text)
    }

    @Test
    fun `an exclusive run can start again once the first has finished`() = runTest {
        var submits = 0
        val viewModel = TestViewModel(
            submit = {
                submits++
                "ok"
            },
        )

        viewModel.onEvent(TestEvent.Submit)
        advanceUntilIdle()
        viewModel.onEvent(TestEvent.Submit)
        advanceUntilIdle()

        assertEquals(2, submits)
    }

    @Test
    fun `the busy flag is up while an exclusive run works and down once it is done`() = runTest {
        val answer = CompletableDeferred<String>()
        val viewModel = TestViewModel(submit = { answer.await() })

        viewModel.onEvent(TestEvent.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.busy)
        viewModel.onEvent(TestEvent.Submit)
        runCurrent()
        assertTrue(viewModel.state.value.busy)

        answer.complete("done")
        advanceUntilIdle()
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun `an unexpected throw takes the busy flag down before onUnexpected hears of it`() = runTest {
        var fail = true
        val viewModel = TestViewModel(submit = { if (fail) error("boom") else "recovered" })

        viewModel.onEvent(TestEvent.Submit)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.busy)
        assertEquals(listOf(false), viewModel.busyWhenUnexpected)

        fail = false
        viewModel.onEvent(TestEvent.Submit)
        advanceUntilIdle()
        assertEquals("recovered", viewModel.state.value.text)
    }

    @Test
    fun `a cancelled exclusive run takes the busy flag down`() = runTest {
        val viewModel = TestViewModel(submit = { CompletableDeferred<String>().await() })

        viewModel.onEvent(TestEvent.Submit)
        runCurrent()
        viewModel.onEvent(TestEvent.CancelSubmit)
        runCurrent()

        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun `nothing first-observed runs before the state is collected, then it runs once`() = runTest {
        val viewModel = TestViewModel()
        advanceUntilIdle()
        // Reading the state is not observing it.
        assertEquals(TestState(), viewModel.state.value)
        assertEquals(0, viewModel.firstObservations)

        val screen = viewModel.state.launchIn(backgroundScope)
        runCurrent()
        assertEquals(1, viewModel.firstObservations)

        val second = viewModel.state.launchIn(backgroundScope)
        runCurrent()
        screen.cancel()
        second.cancel()
        // Past the grace period, as when the screen comes back from the background.
        advanceTimeBy(6.seconds)
        viewModel.state.launchIn(backgroundScope)
        runCurrent()

        assertEquals(1, viewModel.firstObservations)
    }

    @Test
    fun `starting a latest run cancels the one before it`() = runTest {
        val first = CompletableDeferred<String>()
        val second = CompletableDeferred<String>()
        val answers = ArrayDeque(listOf(first, second))
        val viewModel = TestViewModel(load = { answers.removeFirst().await() })

        viewModel.onEvent(TestEvent.Load)
        runCurrent()
        viewModel.onEvent(TestEvent.Load)
        runCurrent()
        // The first answer arrives last, as a slow response to an older request would. It must not
        // overwrite the newer one, and it cannot: its run was cancelled.
        second.complete("second")
        first.complete("first")
        advanceUntilIdle()

        assertEquals("second", viewModel.state.value.text)
    }

    @Test
    fun `cancel stops the run under a key`() = runTest {
        val answer = CompletableDeferred<String>()
        val viewModel = TestViewModel(load = { answer.await() })

        viewModel.onEvent(TestEvent.Load)
        runCurrent()
        viewModel.onEvent(TestEvent.CancelLoad)
        answer.complete("too late")
        advanceUntilIdle()

        assertEquals("", viewModel.state.value.text)
    }

    @Test
    fun `observe collects only while the state is observed, with a grace period`() = runTest {
        val source = MutableSharedFlow<String>()
        val viewModel = TestViewModel(observed = source)
        runCurrent()
        assertEquals(0, source.subscriptionCount.value)

        val screen = viewModel.state.launchIn(backgroundScope)
        runCurrent()
        source.emit("seen")
        runCurrent()
        assertEquals("seen", viewModel.state.value.text)

        screen.cancel()
        advanceTimeBy(4.seconds)
        assertEquals(1, source.subscriptionCount.value)
        advanceTimeBy(2.seconds)
        assertEquals(0, source.subscriptionCount.value)

        viewModel.state.launchIn(backgroundScope)
        runCurrent()
        source.emit("again")
        runCurrent()
        assertEquals("again", viewModel.state.value.text)
    }

    @Test
    fun `an exception in launched work lands in onUnexpected and the next run still works`() =
        runTest {
            var fail = true
            val viewModel = TestViewModel(load = { if (fail) error("boom") else "recovered" })

            viewModel.onEvent(TestEvent.Load)
            advanceUntilIdle()
            assertTrue(viewModel.unexpected.single().cause is IllegalStateException)

            fail = false
            viewModel.onEvent(TestEvent.Load)
            advanceUntilIdle()
            assertEquals("recovered", viewModel.state.value.text)
        }

    @Test
    fun `the same message twice is two messages`() = runTest {
        val viewModel = TestViewModel()

        viewModel.messages.test {
            viewModel.onEvent(TestEvent.Saved)
            viewModel.onEvent(TestEvent.Saved)

            val first = awaitItem()
            val second = awaitItem()
            assertEquals(first.text, second.text)
            assertTrue(first.id != second.id)
        }
    }

    private data class TestState(val text: String = "", val busy: Boolean = false) : UiState

    private sealed interface TestEvent : UiEvent {
        data object Load : TestEvent
        data object CancelLoad : TestEvent
        data object Submit : TestEvent
        data object CancelSubmit : TestEvent
        data object Back : TestEvent
        data object Saved : TestEvent
    }

    private sealed interface TestEffect : UiEffect {
        data object NavigateBack : TestEffect
    }

    private class TestViewModel(
        private val load: suspend () -> String = { "" },
        private val submit: suspend () -> String = { "" },
        observed: Flow<String>? = null,
    ) : MviViewModel<TestState, TestEvent, TestEffect>(TestState()) {

        val unexpected = mutableListOf<AppError.Unexpected>()
        val busyWhenUnexpected = mutableListOf<Boolean>()
        var firstObservations = 0
            private set

        init {
            observed?.let { observe(it) { value -> copy(text = value) } }
            onFirstObserved { firstObservations++ }
        }

        override fun handleEvent(event: TestEvent) {
            when (event) {
                TestEvent.Load -> launchLatest("load") {
                    val text = load()
                    setState { copy(text = text) }
                }

                TestEvent.CancelLoad -> cancel("load")

                TestEvent.Submit -> launchExclusive("submit", busy = { copy(busy = it) }) {
                    val text = submit()
                    setState { copy(text = text) }
                }

                TestEvent.CancelSubmit -> cancel("submit")

                TestEvent.Back -> emit(TestEffect.NavigateBack)

                TestEvent.Saved -> showMessage(UiText.Dynamic("Saved"))
            }
        }

        override fun onUnexpected(error: AppError.Unexpected) {
            unexpected += error
            busyWhenUnexpected += current.busy
        }
    }
}
