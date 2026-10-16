package com.base.app.feature.auth

import androidx.annotation.StringRes
import com.base.app.core.common.AppResult
import com.base.app.core.common.util.UiText
import com.base.app.core.common.userMessage

/**
 * What to tell the person after a failed auth request. The two failures a form actually produces
 * get sentences they can act on; anything else shows the server's message, or [fallback].
 */
internal fun AppResult.Failure.authMessage(@StringRes fallback: Int): UiText = when (code) {
    HTTP_UNAUTHORIZED -> UiText.of(R.string.auth_wrong_credentials)
    HTTP_TOO_MANY_REQUESTS -> UiText.of(R.string.auth_too_many_attempts)
    else -> userMessage(UiText.of(fallback))
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_TOO_MANY_REQUESTS = 429
