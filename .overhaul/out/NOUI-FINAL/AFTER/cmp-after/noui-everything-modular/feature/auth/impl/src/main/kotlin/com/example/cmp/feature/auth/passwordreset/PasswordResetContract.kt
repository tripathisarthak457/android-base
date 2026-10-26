package com.example.cmp.feature.auth.passwordreset

import androidx.compose.runtime.Immutable
import com.example.cmp.core.mvi.UiEffect
import com.example.cmp.core.mvi.UiEvent
import com.example.cmp.core.mvi.UiState
import com.example.cmp.core.mvi.UiText

@Immutable
internal data class PasswordResetState(
    val emailError: UiText? = null,
    val canSubmit: Boolean = false,
    val submitting: Boolean = false,
    val error: UiText? = null,
    val sent: Boolean = false,
) : UiState

internal sealed interface PasswordResetEvent : UiEvent {
    data object Submit : PasswordResetEvent
    data object BackClicked : PasswordResetEvent
}

internal sealed interface PasswordResetEffect : UiEffect {
    data object NavigateBack : PasswordResetEffect
}
