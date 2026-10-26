package com.example.cmp.core.mvi

import kotlin.uuid.Uuid

/**
 * A transient message: the snackbar channel, separate from both state and effects. [id] makes every
 * showing distinct, so "Saved" emitted twice is shown twice instead of being swallowed as an equal
 * value that neither changes the host's state nor restarts its timer. It is random rather than
 * counted so that a message made on any thread needs no shared counter: the standard library's
 * atomics are still experimental.
 */
data class UiMessage(
    val text: UiText,
    val title: UiText? = null,
    val kind: MessageKind = MessageKind.Info,
    val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    val action: MessageAction? = null,
    val id: Uuid = Uuid.random(),
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 3_500L
    }
}

enum class MessageKind { Success, Error, Warning, Info }

/** An optional button on the message: "Retry", "Undo". */
data class MessageAction(val label: UiText, val onClick: () -> Unit)
