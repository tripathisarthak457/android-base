package com.base.app.data.auth.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format for authentication.
 *
 * Separate from anything the app holds in memory, because these are the backend's field names on
 * the backend's schedule. When they rename `access_token`, the change lands here and nowhere else.
 *
 * `expiresIn` is a duration in seconds, which is what OAuth-shaped APIs send. It is converted to
 * an absolute instant the moment it arrives — a duration stored on disk is wrong by however long
 * the app was closed, and the symptom is a session that expires early on the first cold start.
 */
@Serializable
data class TokenResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
)

@Serializable
data class SignInRequestDto(
    val email: String,
    val password: String,
)

@Serializable
data class SignUpRequestDto(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class EmailRequestDto(
    val email: String,
)
