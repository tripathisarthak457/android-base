package com.base.app.data.profile.api

// <opt:media>
import com.base.app.core.common.result.AppResult
// </opt:media>
import com.base.app.data.profile.api.model.Profile
import kotlinx.coroutines.flow.Flow

/** The signed-in person's own profile, kept on the device. */
interface ProfileRepository {

    val profile: Flow<Profile>

    suspend fun save(name: String, bio: String)
    // <opt:media>

    /**
     * Copies the picked image into the app's own storage, shrunk to avatar size, and makes it the
     * avatar; null removes it. A copy, because the photo picker's grant on [picked] ends with the
     * process, and an avatar that disappears after a restart is the bug this prevents.
     */
    suspend fun setPhoto(picked: String?): AppResult<Unit>
    // </opt:media>
}
