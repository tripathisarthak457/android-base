package com.base.app.core.mvi

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * A transient message: the snackbar channel, separate from both state and effects. [id] makes every
 * showing distinct, so "Saved" emitted twice is shown twice instead of being swallowed as an equal
 * value that neither changes the host's state nor restarts its timer.
 */
@OptIn(ExperimentalAtomicApi::class)
data class UiMessage(
    val text: UiText,
    val title: UiText? = null,
    val kind: MessageKind = MessageKind.Info,
    val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    val action: MessageAction? = null,
    val id: Long = ids.incrementAndFetch(),
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 3_500L
        private val ids = AtomicLong(0)
    }
}

enum class MessageKind { Success, Error, Warning, Info }

/** An optional button on the message: "Retry", "Undo". */
data class MessageAction(
    val label: UiText,
    val onClick: () -> Unit,
)
