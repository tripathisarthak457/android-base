package com.example.finalafter.crui2.feature.profile

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import com.example.finalafter.crui2.core.common.result.AppError
import com.example.finalafter.crui2.core.common.result.errorOrNull
import com.example.finalafter.crui2.core.mvi.MessageKind
import com.example.finalafter.crui2.core.mvi.MviViewModel
import com.example.finalafter.crui2.core.mvi.UiText
import com.example.finalafter.crui2.core.ui.toUiText
import com.example.finalafter.crui2.data.profile.api.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * The fields are edited here and only written on Save, so typing never races the store and a
 * half-typed name is not what the rest of the app sees.
 */
@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val savedStateHandle: SavedStateHandle,
) : MviViewModel<ProfileState, ProfileEvent, ProfileEffect>(ProfileState()) {

    @OptIn(SavedStateHandleSaveableApi::class)
    val name: TextFieldState = savedStateHandle.saveable(NAME, saver = TextFieldState.Saver) { TextFieldState() }

    @OptIn(SavedStateHandleSaveableApi::class)
    val bio: TextFieldState = savedStateHandle.saveable(BIO, saver = TextFieldState.Saver) { TextFieldState() }

    init {
        val typed = snapshotFlow { name.text.toString() to bio.text.toString() }
        observe(combine(repository.profile, typed, ::Pair)) { (profile, fields) ->
            val (name, bio) = fields
            copy(
                saved = profile,
                isDirty = name.trim() != profile.name || bio.trim() != profile.bio,
                nameError = nameError && name.isBlank(),
            )
        }
        onFirstObserved { launchExclusive(FILL) { fillFields() } }
    }

    override fun handleEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Save -> save()
            is ProfileEvent.PhotoPicked -> setPhoto(event.uri)
        }
    }

    private fun setPhoto(uri: String?) {
        launchLatest(PHOTO) {
            repository.setPhoto(uri).errorOrNull()?.let { error ->
                showMessage(text = error.toUiText(), kind = MessageKind.Error)
            }
        }
    }

    /**
     * The stored profile goes into the fields once. After process death the fields come back with
     * what was being typed, and the stored values must not overwrite it.
     */
    private suspend fun fillFields() {
        if (savedStateHandle.get<Boolean>(FILLED) != true) {
            val stored = repository.profile.first()
            name.setTextAndPlaceCursorAtEnd(stored.name)
            bio.setTextAndPlaceCursorAtEnd(stored.bio)
            savedStateHandle[FILLED] = true
        }
        setState { copy(loaded = true) }
    }

    private fun save() {
        val name = name.text.toString()
        val bio = bio.text.toString()
        if (name.isBlank()) {
            setState { copy(nameError = true) }
            return
        }
        launchExclusive(SAVE) {
            repository.save(name, bio)
            showMessage(text = UiText.of(R.string.profile_saved), kind = MessageKind.Success)
        }
    }

    // The profile lives on the device, so a failure here is the store refusing a write, and the
    // person would otherwise believe the change was kept.
    override fun onUnexpected(error: AppError.Unexpected) {
        showMessage(text = error.toUiText(), kind = MessageKind.Error)
    }

    companion object {
        const val NAME_LIMIT = 40
        const val BIO_LIMIT = 160

        private const val NAME = "name"
        private const val BIO = "bio"
        private const val FILLED = "filled"
        private const val FILL = "fill"
        private const val SAVE = "save"
        private const val PHOTO = "photo"
    }
}
