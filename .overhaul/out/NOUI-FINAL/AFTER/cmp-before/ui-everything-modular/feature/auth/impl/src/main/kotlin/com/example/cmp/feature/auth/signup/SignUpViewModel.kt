package com.example.cmp.feature.auth.signup

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.common.validation.ValidationError
import com.example.cmp.core.common.validation.Validators
import com.example.cmp.core.common.validation.and
import com.example.cmp.core.mvi.MviViewModel
import com.example.cmp.core.mvi.UiText
import com.example.cmp.core.ui.form.toUiText
import com.example.cmp.data.auth.api.AuthRepository
import com.example.cmp.data.auth.api.SignUpDetails
import com.example.cmp.feature.auth.R
import com.example.cmp.feature.auth.authMessage
import com.example.cmp.feature.auth.errorFor
import com.example.cmp.feature.auth.fieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop

@HiltViewModel
internal class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SignUpState, SignUpEvent, SignUpEffect>(SignUpState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val name: TextFieldState = savedStateHandle.saveable(NAME, saver = TextFieldState.Saver) { TextFieldState() }

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Not saved: a password does not belong in the bundle the system writes to disk on process death.
    val password = TextFieldState()
    val confirm = TextFieldState()

    private val confirmRule = Validators.required() and Validators.sameAs { password.text }

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // A rule runs only when its text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { name.text.toString() }.drop(1)) {
            copy(nameError = nameRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { password.text.toString() }.drop(1)) {
            copy(passwordError = passwordRule.errorFor(it, showErrors))
        }
        // Keyed on both, so changing the password re-checks the confirmation too.
        observe(snapshotFlow { confirm.text.toString() to password.text.toString() }.drop(1)) { (text, _) ->
            copy(confirmError = confirmError(text))
        }
        observe(snapshotFlow { listOf(name, email, password, confirm).all { it.text.isNotBlank() } }) {
            copy(canSubmit = it)
        }
    }

    override fun handleEvent(event: SignUpEvent) {
        when (event) {
            SignUpEvent.Submit -> submit()
            SignUpEvent.BackClicked -> emit(SignUpEffect.NavigateBack)
        }
    }

    private fun submit() {
        showErrors = true
        val nameError = nameRule.errorFor(name.text, showErrors)
        val emailError = emailRule.errorFor(email.text, showErrors)
        val passwordError = passwordRule.errorFor(password.text, showErrors)
        val confirmError = confirmError(confirm.text)
        setState {
            copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError,
                error = null,
            )
        }
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) return

        launchExclusive(SUBMIT, busy = { copy(submitting = it) }) {
            val result = authRepository.signUp(
                SignUpDetails(
                    name = name.text.toString(),
                    email = email.text.toString(),
                    password = password.text.toString(),
                ),
            )
            when (result) {
                is AppResult.Success -> emit(SignUpEffect.SignedUp)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_sign_up_failed)
            }
        }
    }

    /** "Passwords do not match" says more than the generic mismatch message does. */
    private fun confirmError(text: CharSequence): UiText? {
        if (!showErrors) return null
        return when (val error = confirmRule.validate(text)) {
            ValidationError.Mismatch -> UiText.of(R.string.auth_passwords_do_not_match)
            else -> error?.toUiText()
        }
    }

    /** A validation response lands on the fields it names; anything else goes in the banner. */
    private fun showFailure(error: AppError, @StringRes fallback: Int) {
        val nameError = error.fieldError(NAME)
        val emailError = error.fieldError(EMAIL)
        val passwordError = error.fieldError(PASSWORD)
        setState {
            if (nameError == null && emailError == null && passwordError == null) {
                copy(error = error.authMessage(fallback))
            } else {
                copy(nameError = nameError, emailError = emailError, passwordError = passwordError)
            }
        }
    }

    private companion object {
        // The names the server's validation errors use, and the saved-state keys.
        const val NAME = "name"
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val SUBMIT = "submit"
        const val MIN_NAME_LENGTH = 2

        val nameRule = Validators.required() and Validators.minLength(MIN_NAME_LENGTH)
        val emailRule = Validators.required() and Validators.email()
        val passwordRule = Validators.password()
    }
}
