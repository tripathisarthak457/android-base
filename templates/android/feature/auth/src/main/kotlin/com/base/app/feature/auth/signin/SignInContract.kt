package com.base.app.feature.auth.signin

import androidx.compose.runtime.Immutable
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.core.mvi.UiText

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
    // <opt:googlesignin>
    data class GoogleIdTokenReceived(val idToken: String) : SignInEvent
    data object GoogleFailed : SignInEvent
    // </opt:googlesignin>
}

internal sealed interface SignInEffect : UiEffect {
    data object SignedIn : SignInEffect
    data object OpenSignUp : SignInEffect
    data object OpenPasswordReset : SignInEffect
}
