package com.base.app.core.common.mvi

import com.base.app.core.common.util.UiText

/**
 * A transient message — the snackbar/toast channel, separate from both state and effects.
 *
 * Separate from [UiState] because a message is not part of what the screen *is*; leaving it in
 * state means deciding when to clear it, and forgetting to means it reappears on rotation.
 * Separate from [UiEffect] because every screen needs it, and threading a `ShowMessage` case
 * through every feature's effect type is the same fifteen lines repeated per feature.
 */
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

/**
 * An optional button on the message — "Retry", "Undo".
 *
 * [onClick] is a plain lambda rather than an event, so the ViewModel that raised the message
 * decides what the action does without every screen's event type gaining a case for it.
 */
data class MessageAction(
    val label: UiText,
    val onClick: () -> Unit,
)
