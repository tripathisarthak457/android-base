package com.base.app.data.auth.remote

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
// <opt:googlesignin>

@Serializable
internal data class GoogleSignInRequestDto(
    @SerialName("id_token") val idToken: String,
)
// </opt:googlesignin>
