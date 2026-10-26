package com.base.app.data.profile.api

// <opt:media>
import com.base.app.core.common.result.AppResult
// </opt:media>
import com.base.app.data.profile.api.model.Profile
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [ProfileRepository] in memory that trims what it saves, as the real one does. With
 * [refuseWrites] a save throws as a full disk would, so a test can hold a screen to saying so.
 */
class FakeProfileRepository(
    initial: Profile = Profile(),
    private val refuseWrites: Boolean = false,
) : ProfileRepository {

    private val state = MutableStateFlow(initial)

    /** What is stored now. */
    val stored: Profile get() = state.value

    override val profile: StateFlow<Profile> = state

    override suspend fun save(name: String, bio: String) {
        if (refuseWrites) throw IOException("No space left on device")
        state.value = state.value.copy(name = name.trim(), bio = bio.trim())
    }
    // <opt:media>

    override suspend fun setPhoto(picked: String?): AppResult<Unit> {
        state.value = state.value.copy(photoUri = picked)
        return AppResult.Success(Unit)
    }
    // </opt:media>
}
