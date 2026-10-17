package com.example.none.core.mvi

import java.util.concurrent.atomic.AtomicLong

/**
 * A transient message: the snackbar channel, separate from both state and effects. [id] makes every
 * showing distinct, so "Saved" emitted twice is shown twice instead of being swallowed as an equal
 * value that neither changes the host's state nor restarts its timer.
 */
data class UiMessage(
    val text: UiText,
    val title: UiText? = null,
    val kind: MessageKind = MessageKind.Info,
    val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    val action: MessageAction? = null,
    val id: Long = ids.incrementAndGet(),
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 3_500L
        private val ids = AtomicLong()
    }
}

enum class MessageKind { Success, Error, Warning, Info }

/** An optional button on the message: "Retry", "Undo". */
data class MessageAction(
    val label: UiText,
    val onClick: () -> Unit,
)
