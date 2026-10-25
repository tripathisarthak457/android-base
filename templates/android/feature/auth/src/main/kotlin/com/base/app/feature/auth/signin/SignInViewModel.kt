package com.base.app.feature.auth.signin

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.validation.Validators
import com.base.app.core.common.validation.and
import com.base.app.core.mvi.MviViewModel
// <opt:googlesignin>
import com.base.app.core.mvi.UiText
// </opt:googlesignin>
import com.base.app.data.auth.api.AuthRepository
import com.base.app.feature.auth.R
import com.base.app.feature.auth.authMessage
import com.base.app.feature.auth.errorFor
import com.base.app.feature.auth.fieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import javax.inject.Inject

@HiltViewModel
internal class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<SignInState, SignInEvent, SignInEffect>(SignInState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Not saved: a password does not belong in the bundle the system writes to disk on process death.
    val password = TextFieldState()

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // A rule runs only when its text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { password.text.toString() }.drop(1)) {
            copy(passwordError = passwordRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.isNotBlank() && password.text.isNotEmpty() }) { copy(canSubmit = it) }
    }

    override fun handleEvent(event: SignInEvent) {
        when (event) {
            SignInEvent.Submit -> submit()
            SignInEvent.CreateAccountClicked -> emit(SignInEffect.OpenSignUp)
            SignInEvent.ForgotPasswordClicked -> emit(SignInEffect.OpenPasswordReset)
            // <opt:googlesignin>
            is SignInEvent.GoogleIdTokenReceived -> signInWithGoogle(event.idToken)
            SignInEvent.GoogleFailed -> setState { copy(error = UiText.of(R.string.auth_google_failed)) }
            // </opt:googlesignin>
        }
    }

    private fun submit() {
        showErrors = true
        val emailError = emailRule.errorFor(email.text, showErrors)
        val passwordError = passwordRule.errorFor(password.text, showErrors)
        setState { copy(emailError = emailError, passwordError = passwordError, error = null) }
        if (emailError != null || passwordError != null) return

        launchExclusive(SUBMIT, busy = { copy(submitting = it) }) {
            val result = authRepository.signIn(email = email.text.toString(), password = password.text.toString())
            when (result) {
                is AppResult.Success -> emit(SignInEffect.SignedIn)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_sign_in_failed)
            }
        }
    }
    // <opt:googlesignin>

    private fun signInWithGoogle(idToken: String) {
        setState { copy(error = null) }
        launchExclusive(SUBMIT, busy = { copy(submitting = it) }) {
            val result = authRepository.signInWithGoogle(idToken)
            when (result) {
                is AppResult.Success -> emit(SignInEffect.SignedIn)
                is AppResult.Failure -> showFailure(result.error, R.string.auth_google_sign_in_failed)
            }
        }
    }
    // </opt:googlesignin>

    /** A validation response lands on the fields it names; anything else goes in the banner. */
    private fun showFailure(error: AppError, @StringRes fallback: Int) {
        val emailError = error.fieldError(EMAIL)
        val passwordError = error.fieldError(PASSWORD)
        setState {
            if (emailError == null && passwordError == null) {
                copy(error = error.authMessage(fallback))
            } else {
                copy(emailError = emailError, passwordError = passwordError)
            }
        }
    }

    private companion object {
        // The names the server's validation errors use, and the saved-state key for the email.
        const val EMAIL = "email"
        const val PASSWORD = "password"
        const val SUBMIT = "submit"

        val emailRule = Validators.required() and Validators.email()
        val passwordRule = Validators.required()
    }
}
