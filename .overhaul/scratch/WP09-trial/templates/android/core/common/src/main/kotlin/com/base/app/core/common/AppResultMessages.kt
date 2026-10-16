package com.base.app.core.common

import com.base.app.core.common.util.UiText
import java.io.IOException

/**
 * What to show for a failure: the server's own message when it sent one, otherwise a sentence in
 * the app's language for what actually went wrong — queued, offline, unreachable — and [fallback]
 * for anything else.
 */
fun AppResult.Failure.userMessage(fallback: UiText = UiText.of(R.string.common_error_generic)): UiText =
    message?.takeIf { it.isNotBlank() }?.let(UiText::Dynamic) ?: when {
        queued -> UiText.of(R.string.common_error_queued)
        isOffline -> UiText.of(R.string.common_error_offline)
        code == null && cause.hasCause<IOException>() -> UiText.of(R.string.common_error_unreachable)
        else -> fallback
    }

/** Transport failures arrive wrapped in the network layer's own exception types. */
private inline fun <reified T : Throwable> Throwable?.hasCause(): Boolean =
    generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).any { it is T }

private const val MAX_CAUSE_DEPTH = 8
