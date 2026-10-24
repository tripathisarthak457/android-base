package com.base.app.core.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.base.app.core.common.log.Log
import com.base.app.core.mvi.MessageKind

/**
 * What shows the messages screens raise ("Saved", "Could not refresh"): a toast, a snackbar or a
 * banner, which this project leaves to its design system. Until one is built, each message is
 * written to the log, so a failure someone should have seen is at least on record. The component
 * that shows them provides itself once, around the app's content:
 *
 * ```
 * val toasts = MessagePresenter { message -> toastHost.show(message) }
 * CompositionLocalProvider(LocalMessagePresenter provides toasts) {
 *     …
 * }
 * ```
 */
fun interface MessagePresenter {
    fun present(message: PresentedMessage)
}

/** A message with its text resolved in the current language, ready to show. */
class PresentedMessage(
    val text: String,
    val title: String?,
    val kind: MessageKind,
    val durationMillis: Long,
    val actionLabel: String?,
    val onAction: (() -> Unit)?,
)

val LocalMessagePresenter = staticCompositionLocalOf { LoggingPresenter }

private val LoggingPresenter = MessagePresenter { message ->
    val title = message.title?.let { "$it: " }.orEmpty()
    Log.i("${message.kind}: $title${message.text}", tag = "Message")
}
