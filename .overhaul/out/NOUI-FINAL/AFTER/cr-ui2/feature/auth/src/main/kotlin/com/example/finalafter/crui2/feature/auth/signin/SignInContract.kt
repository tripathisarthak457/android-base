package com.example.finalafter.crui2.feature.auth.signin

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui2.core.mvi.UiEffect
import com.example.finalafter.crui2.core.mvi.UiEvent
import com.example.finalafter.crui2.core.mvi.UiState
import com.example.finalafter.crui2.core.mvi.UiText

/** The fields' text is not here: it lives in the ViewModel's `TextFieldState`s. */
@Immutable
internal data class SignInState(
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
) : UiState

internal sealed interface SignInEvent : UiEvent {
    data object Submit : SignInEvent
    data object CreateAccountClicked : SignInEvent
    data object ForgotPasswordClicked : SignInEvent
    data class GoogleIdTokenReceived(val idToken: String) : SignInEvent
    data object GoogleFailed : SignInEvent
}

internal sealed interface SignInEffect : UiEffect {
    data object SignedIn : SignInEffect
    data object OpenSignUp : SignInEffect
    data object OpenPasswordReset : SignInEffect
}
