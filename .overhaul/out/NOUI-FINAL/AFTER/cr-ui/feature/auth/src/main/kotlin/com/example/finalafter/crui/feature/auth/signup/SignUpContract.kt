package com.example.finalafter.crui.feature.auth.signup

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui.core.mvi.UiEffect
import com.example.finalafter.crui.core.mvi.UiEvent
import com.example.finalafter.crui.core.mvi.UiState
import com.example.finalafter.crui.core.mvi.UiText

/** The fields' text is not here: it lives in the ViewModel's `TextFieldState`s. */
@Immutable
internal data class SignUpState(
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
) : UiState

internal sealed interface SignUpEvent : UiEvent {
    data object Submit : SignUpEvent
    data object BackClicked : SignUpEvent
}

internal sealed interface SignUpEffect : UiEffect {
    data object SignedUp : SignUpEffect
    data object NavigateBack : SignUpEffect
}
