package com.example.finalafter.crui2.feature.auth.passwordreset

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.finalafter.crui2.core.common.result.AppResult
import com.example.finalafter.crui2.core.common.validation.Validators
import com.example.finalafter.crui2.core.common.validation.and
import com.example.finalafter.crui2.core.mvi.MviViewModel
import com.example.finalafter.crui2.data.auth.api.AuthRepository
import com.example.finalafter.crui2.feature.auth.R
import com.example.finalafter.crui2.feature.auth.authMessage
import com.example.finalafter.crui2.feature.auth.errorFor
import com.example.finalafter.crui2.feature.auth.fieldError
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.drop

/**
 * Request a password-reset link. Success says a link *has been sent if the address is registered*,
 * never whether it was.
 */
@HiltViewModel
internal class PasswordResetViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : MviViewModel<PasswordResetState, PasswordResetEvent, PasswordResetEffect>(PasswordResetState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val email: TextFieldState = savedStateHandle.saveable(EMAIL, saver = TextFieldState.Saver) { TextFieldState() }

    // Errors stay quiet until the first submit, then follow the text as it is corrected.
    private var showErrors = false

    init {
        // The rule runs only when the text changes. A collection restarted after the screen was away
        // starts with the text already judged, and judging it again would replace the error the
        // server returned for it.
        observe(snapshotFlow { email.text.toString() }.drop(1)) {
            copy(emailError = emailRule.errorFor(it, showErrors))
        }
        observe(snapshotFlow { email.text.isNotBlank() }) { copy(canSubmit = it) }
    }

    override fun handleEvent(event: PasswordResetEvent) {
        when (event) {
            PasswordResetEvent.Submit -> submit()
            PasswordResetEvent.BackClicked -> emit(PasswordResetEffect.NavigateBack)
        }
    }

    private fun submit() {
        showErrors = true
        val emailError = emailRule.errorFor(email.text, showErrors)
        setState { copy(emailError = emailError, error = null) }
        if (emailError != null) return

        launchExclusive(SUBMIT, busy = { copy(submitting = it) }) {
            when (val result = authRepository.requestPasswordReset(email.text.toString())) {
                is AppResult.Success -> setState { copy(sent = true) }
                is AppResult.Failure -> {
                    val fieldError = result.error.fieldError(EMAIL)
                    setState {
                        if (fieldError != null) {
                            copy(emailError = fieldError)
                        } else {
                            copy(error = result.error.authMessage(R.string.auth_reset_failed))
                        }
                    }
                }
            }
        }
    }

    private companion object {
        // The name the server's validation errors use, and the saved-state key.
        const val EMAIL = "email"
        const val SUBMIT = "submit"

        val emailRule = Validators.required() and Validators.email()
    }
}
