package com.example.finalafter.crnoui.feature.profile

import androidx.compose.runtime.Immutable
import com.example.finalafter.crnoui.core.mvi.UiEffect
import com.example.finalafter.crnoui.core.mvi.UiEvent
import com.example.finalafter.crnoui.core.mvi.UiState
import com.example.finalafter.crnoui.data.profile.api.model.Profile

/** The fields' text is not here: it lives in the ViewModel's `TextFieldState`s. */
@Immutable
internal data class ProfileState(
    val saved: Profile = Profile(),
    val nameError: Boolean = false,
    val isDirty: Boolean = false,
    val loaded: Boolean = false,
) : UiState

internal sealed interface ProfileEvent : UiEvent {
    data object Save : ProfileEvent
    data class PhotoPicked(val uri: String?) : ProfileEvent
}

internal sealed interface ProfileEffect : UiEffect
