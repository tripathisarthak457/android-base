package com.example.wp04.abstd.data.profile.api

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {

    val profile: Flow<Profile>

    suspend fun save(name: String, bio: String)

    suspend fun setPhoto(uri: String?)
}
