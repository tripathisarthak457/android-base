package com.base.app.data.profile

// <opt:media>
import androidx.core.net.toUri
// </opt:media>
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
// <opt:media>
import com.base.app.core.common.coroutines.AppDispatchers
import com.base.app.core.common.result.AppResult
import com.base.app.core.media.compress.ImageCompression
import com.base.app.core.media.compress.ImageCompressor
// </opt:media>
import com.base.app.data.profile.api.ProfileRepository
import com.base.app.data.profile.api.model.Profile
// <opt:media>
import com.base.app.data.profile.di.AvatarDirectory
// </opt:media>
import com.base.app.data.profile.di.ProfileStore
// <opt:media>
import java.io.File
// </opt:media>
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
// <opt:media>
import kotlinx.coroutines.withContext
// </opt:media>

/** The profile, kept on the device in a file of its own that sign-out empties. */
@Singleton
internal class DefaultProfileRepository @Inject constructor(
    @ProfileStore private val store: DataStore<Preferences>,
    // <opt:media>
    private val compressor: ImageCompressor,
    @AvatarDirectory private val avatars: File,
    private val dispatchers: AppDispatchers,
    // </opt:media>
) : ProfileRepository {

    override val profile: Flow<Profile> = store.data.map {
        Profile(
            name = it[NAME].orEmpty(),
            bio = it[BIO].orEmpty(),
            photoUri = it[PHOTO],
        )
    }

    override suspend fun save(name: String, bio: String) {
        store.edit {
            it[NAME] = name.trim()
            it[BIO] = bio.trim()
        }
    }
    // <opt:media>

    override suspend fun setPhoto(picked: String?): AppResult<Unit> {
        if (picked == null) {
            store.edit { it.remove(PHOTO) }
            deleteAvatarsExcept(keep = null)
            return AppResult.Success(Unit)
        }
        // A new name for each photo, so an image loader holding the old one in memory cannot show it.
        val destination = File(avatars, "avatar_${System.nanoTime()}.jpg")
        return when (
            val copied = compressor.compress(
                picked.toUri(),
                ImageCompression.Avatar,
                destination,
            )
        ) {
            is AppResult.Success -> {
                store.edit { it[PHOTO] = copied.value.toUri().toString() }
                deleteAvatarsExcept(keep = copied.value)
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> copied
        }
    }

    private suspend fun deleteAvatarsExcept(keep: File?) = withContext(dispatchers.io) {
        avatars.listFiles()?.filter { it != keep }?.forEach(File::delete)
    }
    // </opt:media>

    private companion object {
        val NAME = stringPreferencesKey("name")
        val BIO = stringPreferencesKey("bio")
        val PHOTO = stringPreferencesKey("photo")
    }
}
