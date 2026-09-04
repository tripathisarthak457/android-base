package com.base.app.core.common.validation

import com.base.app.core.common.util.UiText

/**
 * The outcome of checking one value.
 *
 * A sealed result rather than a nullable error string, so a validator that returns "no error" and
 * one that was never run are different things. That distinction is what lets a form show errors
 * only on fields the user has actually touched.
 */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: UiText) : ValidationResult

    val isValid: Boolean get() = this is Valid
    val errorOrNull: UiText? get() = (this as? Invalid)?.message
}

/**
 * A rule applied to one value.
 *
 * A `fun interface` over a plain lambda so rules compose readably (`Required and Email`) and so a
 * rule can be named at the call site — `Validators.required()` reads as intent where an inline
 * lambda reads as arithmetic.
 */
fun interface Validator<T> {
    fun validate(value: T): ValidationResult
}

/**
 * Runs both, reporting the *first* failure.
 *
 * Showing one message at a time is deliberate. A field that reports "required, and must be an
 * email, and must be under 60 characters" all at once is a field nobody reads, and the second and
 * third messages are usually consequences of the first.
 */
infix fun <T> Validator<T>.and(next: Validator<T>): Validator<T> = Validator { value ->
    when (val first = validate(value)) {
        is ValidationResult.Invalid -> first
        ValidationResult.Valid -> next.validate(value)
    }
}

/**
 * The rules a form needs before it needs a library.
 *
 * Every message is a [UiText], so a validator never holds a `Context` and its output is localised
 * where it is rendered — the same reason ViewModels do not format strings.
 */
object Validators {

    fun required(message: String = "This is required."): Validator<String> = Validator { value ->
        if (value.isNotBlank()) ValidationResult.Valid else invalid(message)
    }

    fun minLength(length: Int, message: String? = null): Validator<String> = Validator { value ->
        if (value.length >= length) {
            ValidationResult.Valid
        } else {
            invalid(message ?: "Must be at least $length characters.")
        }
    }

    fun maxLength(length: Int, message: String? = null): Validator<String> = Validator { value ->
        if (value.length <= length) {
            ValidationResult.Valid
        } else {
            invalid(message ?: "Must be $length characters or fewer.")
        }
    }

    /**
     * A deliberately permissive email check.
     *
     * Full RFC 5322 validation rejects addresses that work and accepts ones that do not; the only
     * authoritative test is sending mail to it. This catches the typos worth catching — a missing
     * `@`, a missing dot, a trailing space — and lets everything else through to the server.
     */
    fun email(message: String = "Enter a valid email address."): Validator<String> =
        Validator { value ->
            if (EMAIL.matches(value.trim())) ValidationResult.Valid else invalid(message)
        }

    /**
     * Digits only, within a length range.
     *
     * Not a country-aware check: phone numbering plans change, and a client that knows them goes
     * out of date silently. Length and digits catch the mistakes a person makes typing.
     */
    fun phone(
        minDigits: Int = 6,
        maxDigits: Int = 15,
        message: String = "Enter a valid phone number.",
    ): Validator<String> = Validator { value ->
        val digits = value.filter(Char::isDigit)
        if (digits.length in minDigits..maxDigits && digits.length == value.count { !it.isWhitespace() }) {
            ValidationResult.Valid
        } else {
            invalid(message)
        }
    }

    fun matches(pattern: Regex, message: String): Validator<String> = Validator { value ->
        if (pattern.matches(value)) ValidationResult.Valid else invalid(message)
    }

    fun numericRange(
        range: LongRange,
        message: String? = null,
    ): Validator<String> = Validator { value ->
        val parsed = value.toLongOrNull()
        when {
            parsed == null -> invalid(message ?: "Enter a number.")
            parsed !in range -> invalid(message ?: "Must be between ${range.first} and ${range.last}.")
            else -> ValidationResult.Valid
        }
    }

    /**
     * A password rule that states what it wants up front.
     *
     * The message lists every requirement rather than reporting them one at a time, because
     * password rules are the one case where drip-feeding failures is genuinely infuriating —
     * the user cannot see the rules and is guessing.
     */
    fun password(
        minLength: Int = 8,
        requireDigit: Boolean = true,
        requireLetter: Boolean = true,
    ): Validator<String> = Validator { value ->
        val failures = buildList {
            if (value.length < minLength) add("$minLength characters")
            if (requireLetter && value.none(Char::isLetter)) add("a letter")
            if (requireDigit && value.none(Char::isDigit)) add("a number")
        }
        if (failures.isEmpty()) {
            ValidationResult.Valid
        } else {
            invalid("Needs at least ${failures.joinToString(", ")}.")
        }
    }

    /** For a confirm-password field, or any value that has to equal another. */
    fun sameAs(other: () -> String, message: String = "Does not match."): Validator<String> =
        Validator { value ->
            if (value == other()) ValidationResult.Valid else invalid(message)
        }

    /** Always passes. The identity, for a field whose rules are decided at runtime. */
    fun <T> none(): Validator<T> = Validator { ValidationResult.Valid }

    private fun invalid(message: String) = ValidationResult.Invalid(UiText.Dynamic(message))

    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
}
