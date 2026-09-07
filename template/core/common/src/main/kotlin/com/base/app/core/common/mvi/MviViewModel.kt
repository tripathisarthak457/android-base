package com.base.app.core.common.mvi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.app.core.common.AppResult
import com.base.app.core.common.util.AppLogger
import com.base.app.core.common.util.UiText
import com.base.app.core.common.util.asUiText
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
 * The base every feature ViewModel extends.
 *
 * ## Events are queued, not launched
 *
 * `onEvent` puts the event on an unbounded channel that a single coroutine drains in order. The
 * obvious alternative — `viewModelScope.launch { handleEvent(event) }` per call — starts a
 * coroutine per event, and two events that both read-modify-write the state can interleave
 * between the read and the write. That is a lost update, it only shows up under fast input, and
 * it is close to impossible to reproduce deliberately. Serialising the handlers removes the
 * possibility rather than making it rarer.
 *
 * The cost is that a slow handler delays the next event. That is the right default — the events
 * behind it almost always depend on what this one is about to write — and anything genuinely
 * long-running opts out explicitly with [launchWork].
 *
 * ## Three output channels
 *
 * [state] is what the screen renders, [effects] are one-shot instructions to the composable, and
 * [messages] is the shared snackbar channel. See [UiState] and [UiMessage] for why the last two
 * are not folded into the first.
 */
abstract class MviViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    /** The state right now. For a handler that needs to read before it writes. */
    protected val currentState: S get() = _state.value

    // Unlimited rather than buffered. A full buffer makes `trySend` fail, and the failure is
    // a navigation that simply never happens with nothing in the log to say why. Effects are
    // small, few, and drained by a collector that is only ever briefly absent.
    private val _effects = Channel<F>(Channel.UNLIMITED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private val events = Channel<E>(Channel.UNLIMITED)

    /** The most recent job per [launchLatest] key, so the next one can cancel it. */
    private val keyedJobs = mutableMapOf<Any, Job>()

    init {
        viewModelScope.launch {
            for (event in events) {
                try {
                    handleEvent(event)
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    // One handler failing must not tear down the loop; every subsequent event on
                    // this screen would be silently dropped and the screen would appear frozen.
                    onError(throwable)
                }
            }
        }
    }

    /**
     * Handles one event. Called from a single coroutine, so implementations never race each
     * other and may read [currentState] safely.
     */
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

    /**
     * Work that must not hold up the event queue — a long upload, a poll, anything the user
     * keeps interacting during.
     *
     * Returns the [Job] so a handler can cancel a previous one. If that is what you are doing —
     * search-as-you-type, a filter, anything the user retriggers — use [launchLatest], which
     * cancels the previous job for you rather than leaving it as something to remember.
     */
    protected fun launchWork(block: suspend CoroutineScope.() -> Unit): Job =
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                onError(throwable)
            }
        }

    /**
     * Brings back the part of the state that cannot be recomputed, after the process was killed.
     *
     * Android kills backgrounded processes routinely, and "Don't keep activities" makes it
     * constant. Navigation survives that already — the back stack is serialised — but a
     * ViewModel's state does not, so somebody who switched apps with a half-filled form comes
     * back to an empty one.
     *
     * ## The screen says what to keep, rather than the base class keeping everything
     *
     * Most of a UiState should not come back. A [LoadState.Error] restored from an hour ago is an
     * error about a request nobody made; the right answer to process death is to run the load
     * again and show what is true now. What has to survive is what the user typed, and only the
     * screen knows which fields those are.
     *
     * Values go into the handle as they are, so they must be the kinds of thing a Bundle can hold
     * — strings, numbers, booleans, Parcelables. That constraint is the reason this takes a map
     * rather than the whole state: it applies to a handful of named fields instead of to every
     * type a UiState might ever contain.
     *
     * ```
     * init {
     *     persistState(
     *         handle = savedStateHandle,
     *         save = { mapOf("query" to it.query) },
     *         restore = { copy(query = it["query"] as? String ?: query) },
     *     )
     * }
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

        // Written as the state changes rather than serialised at save time: these are a few named
        // primitives going into a map, so the write is cheaper than deciding when to do it.
        viewModelScope.launch {
            state.collect { current ->
                save(current).forEach { (key, value) -> handle[PERSIST_PREFIX + key] = value }
            }
        }
    }

    /**
     * Work that replaces whatever was running under the same [key].
     *
     * The case this exists for is search-as-you-type: without it, the response to "ca" can arrive
     * after the response to "cars" and overwrite it, and the screen shows results for a query the
     * user has already moved past. Cancelling the previous job removes the possibility rather
     * than making it less likely.
     *
     * [debounceMillis] delays the start, so a burst of keystrokes issues one request rather than
     * six. The delay is inside the cancellable job on purpose: a keystroke during it cancels the
     * wait as well as the work.
     *
     * Safe without a lock because handlers run on the single event coroutine — see the class
     * documentation. Calling this from somewhere else is not covered by that.
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

    /**
     * Turns a failed [AppResult] into the [LoadState.Error] a screen renders.
     *
     * Centralised so that "the server sent no message" resolves to the same fallback copy
     * everywhere, instead of each screen inventing its own — which is how one screen ends up
     * showing a raw exception class name.
     */
    protected fun AppResult.Failure.toLoadState(
        fallback: UiText = UiText.Dynamic(DEFAULT_ERROR),
    ): LoadState.Error = LoadState.Error(
        message = message?.takeIf { it.isNotBlank() }?.asUiText() ?: fallback,
        isOffline = isOffline,
        code = code,
    )

    /**
     * Last resort for anything thrown out of a handler.
     *
     * Overridable, because some screens have a better answer than a toast — a form can route a
     * validation failure onto the offending field, for instance.
     */
    protected open fun onError(throwable: Throwable) {
        AppLogger.e(tag = this::class.simpleName ?: "ViewModel", message = "Unhandled", throwable = throwable)
        showMessage(
            text = (throwable.message ?: DEFAULT_ERROR).asUiText(),
            kind = MessageKind.Error,
        )
    }

    override fun onCleared() {
        events.close()
    }

    private companion object {
        const val DEFAULT_ERROR = "Something went wrong. Please try again."

        /** Namespaced so a persisted field cannot collide with a navigation argument. */
        const val PERSIST_PREFIX = "mvi:"
    }
}
