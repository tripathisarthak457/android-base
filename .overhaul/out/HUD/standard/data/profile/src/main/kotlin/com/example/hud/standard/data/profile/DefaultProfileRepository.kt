package com.example.hud.standard.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hud.standard.data.profile.api.ProfileRepository
import com.example.hud.standard.data.profile.api.model.Profile
import com.example.hud.standard.data.profile.di.ProfileStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The profile, kept on the device in a file of its own that sign-out empties. */
@Singleton
internal class DefaultProfileRepository @Inject constructor(
    @ProfileStore private val store: DataStore<Preferences>,
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

    private companion object {
        val NAME = stringPreferencesKey("name")
        val BIO = stringPreferencesKey("bio")
        val PHOTO = stringPreferencesKey("photo")
    }
}
