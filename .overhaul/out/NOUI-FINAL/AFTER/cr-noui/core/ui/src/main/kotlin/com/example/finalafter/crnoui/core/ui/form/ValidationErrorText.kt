package com.example.finalafter.crnoui.core.ui.form

import com.example.finalafter.crnoui.core.common.validation.ValidationError
import com.example.finalafter.crnoui.core.mvi.UiText
import com.example.finalafter.crnoui.core.ui.R

/** The sentence a field shows for a failed rule, in the app's language. */
fun ValidationError.toUiText(): UiText = when (this) {
    ValidationError.Required -> UiText.of(R.string.ui_validation_required)
    ValidationError.Email -> UiText.of(R.string.ui_validation_email)
    is ValidationError.TooShort -> UiText.plural(R.plurals.ui_validation_min_length, minLength)
    ValidationError.Mismatch -> UiText.of(R.string.ui_validation_mismatch)
    is ValidationError.PasswordTooShort -> UiText.plural(R.plurals.ui_validation_password_length, minLength)
    ValidationError.PasswordNeedsLetter -> UiText.of(R.string.ui_validation_password_letter)
    ValidationError.PasswordNeedsDigit -> UiText.of(R.string.ui_validation_password_digit)
}
