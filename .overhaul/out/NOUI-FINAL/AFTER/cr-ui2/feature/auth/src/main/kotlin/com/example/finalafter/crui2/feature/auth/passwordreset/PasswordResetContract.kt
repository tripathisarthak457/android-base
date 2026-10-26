package com.example.finalafter.crui2.feature.auth.passwordreset

import androidx.compose.runtime.Immutable
import com.example.finalafter.crui2.core.mvi.UiEffect
import com.example.finalafter.crui2.core.mvi.UiEvent
import com.example.finalafter.crui2.core.mvi.UiState
import com.example.finalafter.crui2.core.mvi.UiText

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
