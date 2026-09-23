package com.base.app.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.datastore.di.SessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class Profile(
    val name: String = "",
    val bio: String = "",
    /** A content URI from the photo picker, or null for the initials avatar. */
    val photoUri: String? = null,
)

interface ProfileRepository {

    val profile: Flow<Profile>

    suspend fun save(name: String, bio: String)

    suspend fun setPhoto(uri: String?)
}

/** The profile, kept on the device in the session store so it goes on sign-out. */
@Singleton
class DefaultProfileRepository @Inject constructor(
    @SessionDataStore private val store: DataStore<Preferences>,
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

    override suspend fun setPhoto(uri: String?) {
        store.edit { if (uri == null) it.remove(PHOTO) else it[PHOTO] = uri }
    }

    private companion object {
        val NAME = stringPreferencesKey("profile.name")
        val BIO = stringPreferencesKey("profile.bio")
        val PHOTO = stringPreferencesKey("profile.photo")
    }
}
