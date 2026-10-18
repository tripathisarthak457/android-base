package com.example.hud.standard.data.profile.api

import com.example.hud.standard.data.profile.api.model.Profile
import kotlinx.coroutines.flow.Flow

/** The signed-in person's own profile, kept on the device. */
interface ProfileRepository {

    val profile: Flow<Profile>

    suspend fun save(name: String, bio: String)
}
