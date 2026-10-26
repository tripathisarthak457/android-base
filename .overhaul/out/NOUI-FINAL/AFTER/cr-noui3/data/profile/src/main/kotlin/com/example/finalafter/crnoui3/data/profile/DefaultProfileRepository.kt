package com.example.finalafter.crnoui3.data.profile

import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.finalafter.crnoui3.core.common.coroutines.AppDispatchers
import com.example.finalafter.crnoui3.core.common.result.AppResult
import com.example.finalafter.crnoui3.core.media.compress.ImageCompression
import com.example.finalafter.crnoui3.core.media.compress.ImageCompressor
import com.example.finalafter.crnoui3.data.profile.api.ProfileRepository
import com.example.finalafter.crnoui3.data.profile.api.model.Profile
import com.example.finalafter.crnoui3.data.profile.di.AvatarDirectory
import com.example.finalafter.crnoui3.data.profile.di.ProfileStore
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** The profile, kept on the device in a file of its own that sign-out empties. */
@Singleton
internal class DefaultProfileRepository @Inject constructor(
    @ProfileStore private val store: DataStore<Preferences>,
    private val compressor: ImageCompressor,
    @AvatarDirectory private val avatars: File,
    private val dispatchers: AppDispatchers,
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

    override suspend fun setPhoto(picked: String?): AppResult<Unit> {
        if (picked == null) {
            store.edit { it.remove(PHOTO) }
            deleteAvatarsExcept(keep = null)
            return AppResult.Success(Unit)
        }
        // A new name for each photo, so an image loader holding the old one in memory cannot show it.
        val destination = File(avatars, "avatar_${System.nanoTime()}.jpg")
        return when (val copied = compressor.compress(picked.toUri(), ImageCompression.Avatar, destination)) {
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

    private companion object {
        val NAME = stringPreferencesKey("name")
        val BIO = stringPreferencesKey("bio")
        val PHOTO = stringPreferencesKey("photo")
    }
}
