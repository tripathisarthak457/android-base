package com.base.app.core.common.validation

import com.base.app.core.common.R
import com.base.app.core.common.util.UiText

/**
 * The outcome of checking one value. A sealed result rather than a nullable error string, so a
 * validator that returns "no error" and one that was never run are different things.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: UiText) : ValidationResult

    val isValid: Boolean get() = this is Valid
    val errorOrNull: UiText? get() = (this as? Invalid)?.message
}

/** A rule applied to one value. */
fun interface Validator<T> {
    fun validate(value: T): ValidationResult
}

/** Runs both, reporting the *first* failure. Showing one message at a time is deliberate. */
infix fun <T> Validator<T>.and(next: Validator<T>): Validator<T> = Validator { value ->
    when (val first = validate(value)) {
        is ValidationResult.Invalid -> first
        ValidationResult.Valid -> next.validate(value)
    }
}

/**
 * The rules a form needs before it needs a library. Messages are resources, so they follow the
 * app's language; pass your own [UiText] to say something more specific.
 */
object Validators {

    fun required(message: UiText = UiText.of(R.string.validation_required)): Validator<String> =
        Validator { value ->
            if (value.isNotBlank()) ValidationResult.Valid else ValidationResult.Invalid(message)
        }

    fun minLength(length: Int, message: UiText? = null): Validator<String> = Validator { value ->
        if (value.length >= length) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(message ?: UiText.plural(R.plurals.validation_min_length, length))
        }
    }

    fun maxLength(length: Int, message: UiText? = null): Validator<String> = Validator { value ->
        if (value.length <= length) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(message ?: UiText.plural(R.plurals.validation_max_length, length))
        }
    }

    /**
     * A deliberately permissive email check. Full RFC 5322 validation rejects addresses that work
     * and accepts ones that do not; the only authoritative test is sending mail to it.
     */
    fun email(message: UiText = UiText.of(R.string.validation_email)): Validator<String> =
        Validator { value ->
            if (EMAIL.matches(value.trim())) ValidationResult.Valid else ValidationResult.Invalid(message)
        }

    /**
     * Digits only, within a length range. Not a country-aware check: phone numbering plans change,
     * and a client that knows them goes out of date silently.
     */
    fun phone(
        minDigits: Int = 6,
        maxDigits: Int = 15,
        message: UiText = UiText.of(R.string.validation_phone),
    ): Validator<String> = Validator { value ->
        val digits = value.filter(Char::isDigit)
        if (digits.length in minDigits..maxDigits && digits.length == value.count { !it.isWhitespace() }) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(message)
        }
    }

    fun matches(pattern: Regex, message: UiText): Validator<String> = Validator { value ->
        if (pattern.matches(value)) ValidationResult.Valid else ValidationResult.Invalid(message)
    }

    fun numericRange(
        range: LongRange,
        message: UiText? = null,
    ): Validator<String> = Validator { value ->
        val parsed = value.toLongOrNull()
        when {
            parsed == null -> ValidationResult.Invalid(message ?: UiText.of(R.string.validation_number))
            parsed !in range -> ValidationResult.Invalid(
                message ?: UiText.of(R.string.validation_range, range.first, range.last),
            )
            else -> ValidationResult.Valid
        }
    }

    /**
     * A password rule. Reports the first unmet requirement, like [and]; show the whole rule as the
     * field's helper text so nobody learns it one failure at a time.
     */
    fun password(
        minLength: Int = 8,
        requireDigit: Boolean = true,
        requireLetter: Boolean = true,
    ): Validator<String> = Validator { value ->
        when {
            value.length < minLength ->
                ValidationResult.Invalid(UiText.plural(R.plurals.validation_password_length, minLength))
            requireLetter && value.none(Char::isLetter) ->
                ValidationResult.Invalid(UiText.of(R.string.validation_password_letter))
            requireDigit && value.none(Char::isDigit) ->
                ValidationResult.Invalid(UiText.of(R.string.validation_password_digit))
            else -> ValidationResult.Valid
        }
    }

    /** For a confirm-password field, or any value that has to equal another. */
    fun sameAs(
        other: () -> String,
        message: UiText = UiText.of(R.string.validation_mismatch),
    ): Validator<String> =
        Validator { value ->
            if (value == other()) ValidationResult.Valid else ValidationResult.Invalid(message)
        }

    /** Always passes. The identity, for a field whose rules are decided at runtime. */
    fun <T> none(): Validator<T> = Validator { ValidationResult.Valid }

    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
}
