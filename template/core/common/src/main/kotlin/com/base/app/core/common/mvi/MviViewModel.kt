package com.base.app.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.app.core.common.AppResult
import com.base.app.core.common.util.AppLogger
import com.base.app.core.common.util.UiText
import com.base.app.core.common.util.asUiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
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

    private val _effects = Channel<F>(Channel.BUFFERED)
    val effects: Flow<F> = _effects.receiveAsFlow()

    private val _messages = Channel<UiMessage>(Channel.BUFFERED)
    val messages: Flow<UiMessage> = _messages.receiveAsFlow()

    private val events = Channel<E>(Channel.UNLIMITED)

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
     * Returns the [Job] so a handler can cancel a previous one; a search-as-you-type handler that
     * does not is a handler that races its own older requests to the state.
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
    }
}
