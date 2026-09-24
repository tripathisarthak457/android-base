package com.base.app.core.common.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.app.core.common.AppResult
import com.base.app.core.common.R
import com.base.app.core.common.userMessage
import com.base.app.core.common.util.AppLogger
import com.base.app.core.common.util.UiText
import com.base.app.core.common.util.asUiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base class for every screen's ViewModel: [state] to render, [effects] to act on once, and
 * [messages] for the snackbar.
 *
 * Events are handled one at a time, in order, on a single coroutine, so two handlers can never
 * interleave a read and a write of the state. Long-running work that should not block the queue
 * goes through [launchWork] or [launchLatest].
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    protected val currentState: S get() = _state.value

    // Unlimited so trySend cannot fail: a dropped effect is a navigation that silently never happens.
    private val _effects = Channel<F>(Channel.UNLIMITED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private val events = Channel<E>(Channel.UNLIMITED)

    private val keyedJobs = mutableMapOf<Any, Job>()

    init {
        viewModelScope.launch {
            for (event in events) {
                try {
                    handleEvent(event)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    // Keep the loop alive, or every later event on this screen is dropped.
                    onError(throwable)
                }
            }
        }
    }

    /** Called for one event at a time, so [currentState] is safe to read and then write. */
    protected abstract suspend fun handleEvent(event: E)

    fun onEvent(event: E) {
        events.trySend(event)
    }

    protected fun updateState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun emitEffect(effect: F) {
        _effects.trySend(effect)
    }

    /** Runs [block] outside the event queue. Errors go to [onError]. */
    protected fun launchWork(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                onError(throwable)
            }
        }

    /**
     * Restores the fields [save] names after process death, and keeps them saved as they change.
     *
     * Only for what the user typed; loaded data should be fetched again. Values must be types a
     * Bundle can hold.
     *
     * ```
     * persistState(
     *     handle = savedStateHandle,
     *     save = { mapOf("query" to it.query) },
     *     restore = { copy(query = it["query"] as? String ?: query) },
     * )
     * ```
     */
    protected fun persistState(
        handle: SavedStateHandle,
        save: (S) -> Map<String, Any?>,
        restore: S.(Map<String, Any?>) -> S,
    ) {
        val keys = save(currentState).keys
        val restored = keys.associateWith { handle.get<Any?>(PERSIST_PREFIX + it) }
        if (restored.values.any { it != null }) {
            updateState { restore(restored) }
        }

        viewModelScope.launch {
            state.collect { current ->
                save(current).forEach { (key, value) -> handle[PERSIST_PREFIX + key] = value }
            }
        }
    }

    /**
     * Like [launchWork], but cancels whatever is still running under the same [key] first, and
     * waits [debounceMillis] before starting.
     *
     * For search-as-you-type and anything else the user retriggers: an answer to an old request
     * can never overwrite a newer one. Call it from [handleEvent]; the key map is not thread-safe.
     */
    protected fun launchLatest(
        key: Any,
        debounceMillis: Long = 0L,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        keyedJobs.remove(key)?.cancel()
        val job = launchWork {
            if (debounceMillis > 0) delay(debounceMillis)
            block()
        }
        keyedJobs[key] = job
        job.invokeOnCompletion { if (keyedJobs[key] === job) keyedJobs.remove(key) }
        return job
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

    protected fun showMessage(text: String, kind: MessageKind = MessageKind.Info) {
        showMessage(text = text.asUiText(), kind = kind)
    }

    /** The error state for a failed request, with a fallback when the server sent no message. */
    protected fun AppResult.Failure.toLoadState(
        fallback: UiText = UiText.of(R.string.common_error_generic),
    ): LoadState.Error = LoadState.Error(
        message = userMessage(fallback),
        isOffline = isOffline,
        code = code,
    )

    /**
     * Anything a handler throws. Logs it and shows a generic message — an exception's own message
     * is written for developers and can leak internals. Override for a better answer.
     */
    protected open fun onError(throwable: Throwable) {
        AppLogger.e(tag = this::class.simpleName ?: "ViewModel", message = "Unhandled", throwable = throwable)
        showMessage(text = UiText.of(R.string.common_error_generic), kind = MessageKind.Error)
    }

    override fun onCleared() {
        events.close()
        super.onCleared()
    }

    private companion object {
        // Namespaced so a persisted field cannot collide with a navigation argument.
        const val PERSIST_PREFIX = "mvi:"
    }
}
