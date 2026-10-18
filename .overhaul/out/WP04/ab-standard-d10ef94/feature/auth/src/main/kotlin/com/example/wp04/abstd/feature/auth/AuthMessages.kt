package com.example.wp04.abstd.feature.auth

import androidx.annotation.StringRes
import com.example.wp04.abstd.core.common.result.AppError
import com.example.wp04.abstd.core.common.validation.Validator
import com.example.wp04.abstd.core.mvi.UiText
import com.example.wp04.abstd.core.ui.form.toUiText
import com.example.wp04.abstd.core.ui.toUiText

/**
 * What to tell the person after a failed auth request. The two failures a form actually produces
 * get sentences they can act on; anything else shows the server's message, or [fallback].
 */
internal fun AppError.authMessage(@StringRes fallback: Int): UiText = when {
    this is AppError.Http && status == HTTP_UNAUTHORIZED -> UiText.of(R.string.auth_wrong_credentials)
    this is AppError.Http && status == HTTP_TOO_MANY_REQUESTS -> UiText.of(R.string.auth_too_many_attempts)
    else -> toUiText(UiText.of(fallback))
}

/** The server's message for [field] from a validation response, keyed by the name the request used. */
internal fun AppError.fieldError(field: String): UiText? =
    (this as? AppError.Http)?.fieldErrors?.get(field)?.firstOrNull { it.isNotBlank() }?.let(UiText::Dynamic)

/**
 * A field's message: nothing until the person has tried to submit once ([shown]), then whatever
 * the rule says about the text as they correct it.
 */
internal fun Validator<CharSequence>.errorFor(text: CharSequence, shown: Boolean): UiText? =
    if (shown) validate(text)?.toUiText() else null

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_TOO_MANY_REQUESTS = 429
