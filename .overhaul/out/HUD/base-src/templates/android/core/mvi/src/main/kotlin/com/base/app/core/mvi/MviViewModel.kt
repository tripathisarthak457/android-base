package com.base.app.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.app.core.common.log.Log
import com.base.app.core.common.result.AppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Base class for every screen's ViewModel: [state] to render, [effects] to act on once, and
 * [messages] for the snackbar.
 *
 * Events are handled on the main thread and [handleEvent] cannot suspend, so a read of [current]
 * followed by [setState] is never interleaved with another handler, and nothing a handler starts can
 * make Back or a keystroke wait. Slow work goes through [launchLatest] or [launchExclusive], and
 * repository flows through [observe].
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(initial: S) : ViewModel() {

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    // Touched only on the main thread: every job runs on viewModelScope (Main.immediate) and removes
    // itself from its own body, never from a completion handler that could run on another thread.
    private val jobs = HashMap<Any, Job>()

    /** Runs on the main thread and never suspends, so slow work can never hold up Back or a keystroke. */
    fun onEvent(event: E) = handleEvent(event)

    protected abstract fun handleEvent(event: E)

    protected val current: S get() = _state.value

    protected fun setState(reduce: S.() -> S) = _state.update(reduce)

    protected fun emit(effect: F) {
        _effects.trySend(effect)
    }

    protected fun showMessage(
        text: UiText,
        title: UiText? = null,
        kind: MessageKind = MessageKind.Info,
        durationMillis: Long = UiMessage.DEFAULT_DURATION_MILLIS,
        action: MessageAction? = null,
    ) {
        _messages.trySend(UiMessage(text, title, kind, durationMillis, action))
    }

    /** Latest wins: starting again under [key] cancels the previous run (search, reload, paging). */
    protected fun launchLatest(
        key: Any,
        debounce: Duration = Duration.ZERO,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        jobs.remove(key)?.cancel()
        return launchKeyed(key) {
            if (debounce.isPositive()) delay(debounce)
            block()
        }
    }

    /** Exclusive: ignored while a run under [key] is active (submit, pay, save). */
    protected fun launchExclusive(key: Any, block: suspend CoroutineScope.() -> Unit): Job? {
        if (jobs[key]?.isActive == true) return null
        return launchKeyed(key, block)
    }

    protected fun cancel(key: Any) {
        jobs.remove(key)?.cancel()
    }

    /**
     * Collects [flow] into the state only while the UI observes [state], with a grace period so a
     * rotation does not restart it. A collector started in `init` would keep running while the
     * screen sits in the back stack or the app is in the background.
     */
    protected fun <T> observe(flow: Flow<T>, reduce: S.(T) -> S) {
        viewModelScope.launch {
            WhileObserved.command(_state.subscriptionCount).collectLatest { command ->
                if (command == SharingCommand.START) {
                    guarded { flow.collect { value -> _state.update { it.reduce(value) } } }
                }
            }
        }
    }

    /** Unexpected exceptions from launched work land here as [AppError.Unexpected], after logging. */
    protected open fun onUnexpected(error: AppError.Unexpected) {}

    private fun launchKeyed(key: Any, block: suspend CoroutineScope.() -> Unit): Job {
        // Lazy, so the job is in the map before its body runs: on Main.immediate a body that never
        // suspends runs to the end inside start(), and must find itself there to remove itself.
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                guarded { block() }
            } finally {
                if (jobs[key] === coroutineContext.job) jobs.remove(key)
            }
        }
        jobs[key] = job
        job.start()
        return job
    }

    private inline fun guarded(block: () -> Unit) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Log.e("Unhandled in ${this::class.simpleName}", throwable)
            onUnexpected(AppError.Unexpected(throwable))
        }
    }

    private companion object {
        val WhileObserved = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5.seconds.inWholeMilliseconds)
    }
}
