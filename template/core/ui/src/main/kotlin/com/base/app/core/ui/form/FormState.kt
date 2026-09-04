package com.base.app.core.ui.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import com.base.app.core.common.util.UiText
import com.base.app.core.common.validation.ValidationResult
import com.base.app.core.common.validation.Validator

/**
 * One field: its value, whether it has been touched, and its rule.
 *
 * ## "Touched" is the whole point
 *
 * A form that validates on every keystroke tells you your email is invalid after you have typed
 * the letter "a", which is both true and useless. A field is validated as you type only *after*
 * you have left it once — so the first pass is quiet, and correcting a mistake gives immediate
 * feedback. That is the behaviour every well-built form has and almost no hand-rolled one does,
 * because it needs this flag and nobody adds it up front.
 */
@Stable
class FieldState internal constructor(
    initialValue: String,
    private val validator: Validator<String>,
) {
    var value by mutableStateOf(initialValue)
        private set

    /** True once the field has been left, or once submit has been attempted. */
    var touched by mutableStateOf(false)
        internal set

    /** Set from the server's 422 response; cleared as soon as the user edits the field. */
    var serverError by mutableStateOf<UiText?>(null)
        internal set

    val result: ValidationResult get() = validator.validate(value)

    val isValid: Boolean get() = result.isValid

    /** The message to render, or null. Silent until the field has been touched. */
    val error: UiText?
        get() = serverError ?: result.errorOrNull.takeIf { touched }

    fun onChange(newValue: String) {
        value = newValue
        // A server error is about the value that was submitted. The moment the user changes it,
        // the message is about something that no longer exists on screen.
        serverError = null
    }

    fun onFocusLost() {
        touched = true
    }

    internal fun reset(to: String = "") {
        value = to
        touched = false
        serverError = null
    }
}

/**
 * A whole form: named fields, and whether it can be submitted.
 *
 * ## Submission is blocked by validity, not by a flag
 *
 * [isValid] is derived from the fields, so there is no separate boolean to keep in sync and no
 * way for the button to be enabled while a field is wrong.
 *
 * ## Server errors land on the right field
 *
 * [applyServerErrors] takes the `fieldErrors` map straight off `AppResult.Failure` and routes each
 * message to its field. Without it, a 422 that names three fields becomes one snackbar saying
 * "validation failed" and the user has to guess which of the eight inputs it meant.
 */
@Stable
class FormState internal constructor(
    private val fields: Map<String, FieldState>,
) {
    var isSubmitting by mutableStateOf(false)
        internal set

    operator fun get(name: String): FieldState =
        fields[name] ?: error("No field named '$name' in this form. Declared: ${fields.keys}.")

    val isValid: Boolean get() = fields.values.all { it.isValid }

    /** Field name to current value, for building the request body. */
    fun values(): Map<String, String> = fields.mapValues { it.value.value }

    /**
     * Marks everything touched so every failing field shows its message at once.
     *
     * Called on a submit attempt. Validating only the first failure on submit makes the user fix
     * and re-submit repeatedly to discover the rest.
     */
    fun touchAll() {
        fields.values.forEach { it.touched = true }
    }

    /** True when the form may be submitted: valid, and not already in flight. */
    fun canSubmit(): Boolean = isValid && !isSubmitting

    /**
     * Runs [block] only if the form validates, marking everything touched if it does not.
     *
     * The single call a submit button makes, so "validate then submit" cannot be written the
     * wrong way round — which is how a form ends up submitting invalid data and relying on the
     * server to reject it.
     */
    fun submit(block: (Map<String, String>) -> Unit) {
        touchAll()
        if (!isValid || isSubmitting) return
        block(values())
    }

    fun applyServerErrors(fieldErrors: Map<String, List<String>>) {
        fieldErrors.forEach { (name, messages) ->
            val message = messages.firstOrNull()?.takeIf { it.isNotBlank() } ?: return@forEach
            fields[name]?.let {
                it.serverError = UiText.Dynamic(message)
                it.touched = true
            }
        }
    }

    fun reset(values: Map<String, String> = emptyMap()) {
        fields.forEach { (name, field) -> field.reset(values[name].orEmpty()) }
        isSubmitting = false
    }
}

/** Builder for [buildForm]. */
class FormBuilder internal constructor() {
    private val fields = LinkedHashMap<String, FieldState>()

    /**
     * Declares a field.
     *
     * [name] is also the key the server's `fieldErrors` uses, so keeping the two identical is
     * what makes [FormState.applyServerErrors] work with no mapping table.
     */
    fun field(
        name: String,
        initial: String = "",
        validator: Validator<String> = Validator { ValidationResult.Valid },
    ) {
        require(fields.put(name, FieldState(initial, validator)) == null) {
            "Field '$name' is declared twice."
        }
    }

    internal fun build(): FormState = FormState(fields.toMap())
}

/**
 * Declares a form once, in a ViewModel's constructor or `init`.
 *
 * ```
 * private val form = buildForm {
 *     field("email", validator = Validators.required() and Validators.email())
 *     field("password", validator = Validators.password())
 * }
 * ```
 *
 * Held by the ViewModel rather than remembered in the composable, so the values survive a
 * configuration change without a `rememberSaveable` per field — and so the submit logic that
 * reads them lives next to the code that sends them.
 */
fun buildForm(block: FormBuilder.() -> Unit): FormState =
    FormBuilder().apply(block).build()

/**
 * Validates, then runs [block] with the field values while holding the submitting flag.
 *
 * Returns null when the form did not validate, so the caller's `?: return` is the whole of its
 * guard clause.
 *
 * The flag is cleared in a `finally`, which is the reason this exists rather than three lines in
 * each ViewModel: a request that throws — or a screen closed mid-flight, which cancels the
 * coroutine — otherwise leaves the form disabled forever, and it is the failure path nobody
 * tests.
 */
suspend fun <T> FormState.submitting(block: suspend (Map<String, String>) -> T): T? {
    touchAll()
    if (!canSubmit()) return null

    isSubmitting = true
    return try {
        block(values())
    } finally {
        isSubmitting = false
    }
}

/**
 * Marks [field] touched when the user leaves it.
 *
 * The naive `onFocusChanged { if (!it.isFocused) field.onFocusLost() }` fires on the very first
 * composition, before the field has ever been focused — which marks every field touched on entry
 * and shows "This is required." on an untouched empty form. Remembering that it *was* focused is
 * what makes "left the field" mean what it says.
 */
@Composable
fun Modifier.touchOnFocusLost(field: FieldState): Modifier {
    var wasFocused by remember { mutableStateOf(false) }
    return onFocusChanged { state ->
        if (state.isFocused) {
            wasFocused = true
        } else if (wasFocused) {
            wasFocused = false
            field.onFocusLost()
        }
    }
}
