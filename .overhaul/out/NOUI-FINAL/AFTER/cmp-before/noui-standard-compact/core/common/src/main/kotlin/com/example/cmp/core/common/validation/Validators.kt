package com.example.cmp.core.common.validation

/**
 * Why a value fails its rule. A code rather than a sentence: the words belong to the UI layer,
 * which knows the language the person reads.
 */
sealed interface ValidationError {
    data object Required : ValidationError
    data object Email : ValidationError
    data class TooShort(val minLength: Int) : ValidationError
    data object Mismatch : ValidationError
    data class PasswordTooShort(val minLength: Int) : ValidationError
    data object PasswordNeedsLetter : ValidationError
    data object PasswordNeedsDigit : ValidationError
}

/** A rule over one value: null when the value passes, otherwise why it does not. */
fun interface Validator<in T> {
    fun validate(value: T): ValidationError?
}

/** Runs both, reporting the first failure. One message at a time is deliberate. */
infix fun <T> Validator<T>.and(next: Validator<T>): Validator<T> =
    Validator { value -> validate(value) ?: next.validate(value) }

/** The rules a form needs before it needs a library. They take `CharSequence`, so a field's text is checked without a copy. */
object Validators {

    fun required(): Validator<CharSequence> =
        Validator { value -> if (value.isNotBlank()) null else ValidationError.Required }

    fun minLength(length: Int): Validator<CharSequence> =
        Validator { value -> if (value.trim().length >= length) null else ValidationError.TooShort(length) }

    /**
     * A deliberately permissive email check. Full RFC 5322 validation rejects addresses that work
     * and accepts ones that do not; the only authoritative test is sending mail to it.
     */
    fun email(): Validator<CharSequence> =
        Validator { value -> if (EMAIL.matches(value.trim())) null else ValidationError.Email }

    /**
     * Reports the first unmet requirement, like [and]; show the whole rule as the field's helper
     * text so nobody learns it one failure at a time.
     */
    fun password(
        minLength: Int = 8,
        requireDigit: Boolean = true,
        requireLetter: Boolean = true,
    ): Validator<CharSequence> = Validator { value ->
        when {
            value.length < minLength -> ValidationError.PasswordTooShort(minLength)
            requireLetter && value.none(Char::isLetter) -> ValidationError.PasswordNeedsLetter
            requireDigit && value.none(Char::isDigit) -> ValidationError.PasswordNeedsDigit
            else -> null
        }
    }

    /** For a confirm-password field, or any value that has to equal another. */
    fun sameAs(other: () -> CharSequence): Validator<CharSequence> =
        Validator { value -> if (value.contentEquals(other())) null else ValidationError.Mismatch }

    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
}
