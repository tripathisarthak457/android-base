package com.base.app.data.auth.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire format for authentication. Separate from anything the app holds in memory, because these
 * are the backend's field names on the backend's schedule.
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
// <opt:googlesignin>

@Serializable
data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
)
// </opt:googlesignin>
