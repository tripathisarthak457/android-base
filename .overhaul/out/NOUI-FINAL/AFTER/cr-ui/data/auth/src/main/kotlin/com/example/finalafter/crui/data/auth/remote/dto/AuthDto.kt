package com.example.finalafter.crui.data.auth.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format for authentication. Separate from anything the app holds in memory, because these
 * are the backend's field names on the backend's schedule.
 */
@Serializable
internal data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    /** Who signed in. When the server sends it, signing back in after a session ends keeps this person's queued work. */
    @SerialName("user_id") val userId: String? = null,
)

@Serializable
internal data class SignInRequestDto(
    val email: String,
    val password: String,
)

@Serializable
internal data class SignUpRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
internal data class EmailRequestDto(
    val email: String,
)

@Serializable
internal data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
)
