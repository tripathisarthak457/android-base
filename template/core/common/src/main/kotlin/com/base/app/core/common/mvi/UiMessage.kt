package com.base.app.core.common.mvi

import com.base.app.core.common.util.UiText

/** A transient message — the snackbar/toast channel, separate from both state and effects. */
data class UiMessage(
    val text: UiText,
    val title: UiText? = null,
    val kind: MessageKind = MessageKind.Info,
    val durationMillis: Long = DEFAULT_DURATION_MILLIS,
    val action: MessageAction? = null,
) {
    companion object {
        const val DEFAULT_DURATION_MILLIS = 3_500L
        const val LONG_DURATION_MILLIS = 6_000L
    }
}

enum class MessageKind { Success, Error, Warning, Info }

/** An optional button on the message — "Retry", "Undo". */
data class MessageAction(
    val label: UiText,
    val onClick: () -> Unit,
)
