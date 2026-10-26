package com.example.finalafter.crnoui3.core.ui.form

import com.example.finalafter.crnoui3.core.common.validation.ValidationError
import com.example.finalafter.crnoui3.core.mvi.UiText
import com.example.finalafter.crnoui3.core.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationErrorTextTest {

    @Test
    fun `every rule a field can fail has its own sentence`() {
        val expected = mapOf(
            ValidationError.Required to UiText.of(R.string.ui_validation_required),
            ValidationError.Email to UiText.of(R.string.ui_validation_email),
            ValidationError.Mismatch to UiText.of(R.string.ui_validation_mismatch),
            ValidationError.PasswordNeedsLetter to
                UiText.of(R.string.ui_validation_password_letter),
            ValidationError.PasswordNeedsDigit to UiText.of(R.string.ui_validation_password_digit),
        )

        expected.forEach { (error, text) -> assertEquals("$error", text, error.toUiText()) }
    }

    @Test
    fun `a length rule is worded as a plural of the length it asks for`() {
        assertEquals(
            UiText.plural(R.plurals.ui_validation_min_length, 3),
            ValidationError.TooShort(3).toUiText(),
        )
        assertEquals(
            UiText.plural(R.plurals.ui_validation_password_length, 12),
            ValidationError.PasswordTooShort(12).toUiText(),
        )
    }
}
