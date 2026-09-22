package com.base.app.feature.profile

import androidx.compose.runtime.Immutable
import com.base.app.core.common.mvi.MessageKind
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.common.util.UiText
import com.base.app.data.profile.Profile
import com.base.app.data.profile.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Immutable
data class ProfileState(
    val saved: Profile = Profile(),
    val name: String = "",
    val bio: String = "",
    val nameError: Boolean = false,
    val loaded: Boolean = false,
) : UiState {
    val isDirty: Boolean get() = name.trim() != saved.name || bio.trim() != saved.bio
}

sealed interface ProfileEvent : UiEvent {
    data class NameChanged(val value: String) : ProfileEvent
    data class BioChanged(val value: String) : ProfileEvent
    data object Save : ProfileEvent
    data class PhotoPicked(val uri: String?) : ProfileEvent
}

sealed interface ProfileEffect : UiEffect

/**
 * The fields are edited here and only written on Save, so typing never races the store and a
 * half-typed name is not what the rest of the app sees. The photo is the exception: picking one
 * is already a deliberate act, and asking for a second tap to keep it is one tap too many.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
) : MviViewModel<ProfileState, ProfileEvent, ProfileEffect>(ProfileState()) {

    init {
        launchWork {
            val first = repository.profile.first()
            updateState { copy(saved = first, name = first.name, bio = first.bio, loaded = true) }
            repository.profile.collect { updateState { copy(saved = it) } }
        }
    }

    override suspend fun handleEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.NameChanged -> updateState {
                copy(name = event.value.take(NAME_LIMIT), nameError = false)
            }

            is ProfileEvent.BioChanged -> updateState { copy(bio = event.value.take(BIO_LIMIT)) }

            ProfileEvent.Save -> {
                if (currentState.name.isBlank()) {
                    updateState { copy(nameError = true) }
                    return
                }
                repository.save(currentState.name, currentState.bio)
                showMessage(text = UiText.of(R.string.profile_saved), kind = MessageKind.Success)
            }

            is ProfileEvent.PhotoPicked -> repository.setPhoto(event.uri)
        }
    }

    companion object {
        const val NAME_LIMIT = 40
        const val BIO_LIMIT = 160
    }
}
