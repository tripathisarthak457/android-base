package com.base.app.feature.profile

import androidx.compose.runtime.Immutable
import com.base.app.core.mvi.UiEffect
import com.base.app.core.mvi.UiEvent
import com.base.app.core.mvi.UiState
import com.base.app.data.profile.api.model.Profile

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

    // <opt:media>
    data class PhotoPicked(val uri: String?) : ProfileEvent
    // </opt:media>
}

internal sealed interface ProfileEffect : UiEffect
