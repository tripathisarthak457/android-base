package com.example.wp04.abstd.data.auth

import com.example.wp04.abstd.core.common.result.AppResult
import com.example.wp04.abstd.core.common.result.map
import com.example.wp04.abstd.core.common.time.AppClock
import com.example.wp04.abstd.core.datastore.AuthTokenStore
import com.example.wp04.abstd.core.network.NetworkClient
import com.example.wp04.abstd.core.network.NetworkJson
import com.example.wp04.abstd.core.network.model.HttpMethodType
import com.example.wp04.abstd.core.network.model.NetworkRequest
import com.example.wp04.abstd.core.network.post
import com.example.wp04.abstd.data.auth.api.AuthRepository
import com.example.wp04.abstd.data.auth.api.SignUpDetails
import com.example.wp04.abstd.data.auth.remote.EmailRequestDto
import com.example.wp04.abstd.data.auth.remote.SignInRequestDto
import com.example.wp04.abstd.data.auth.remote.SignUpRequestDto
import com.example.wp04.abstd.data.auth.remote.TokenResponseDto
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.encodeToJsonElement
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Talks to the auth endpoints and puts what comes back in the token store.
 *
 * Every call passes `requiresAuth = false`: with a bearer token attached, a wrong password would
 * trigger a token refresh and surface as a session expiry instead.
 */
@Singleton
internal class DefaultAuthRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val tokenStore: AuthTokenStore,
    private val clock: AppClock,
) : AuthRepository {

    override val isSignedIn: Flow<Boolean> = tokenStore.isAuthenticated

    override suspend fun signIn(email: String, password: String): AppResult<Unit> =
        networkClient.post<SignInRequestDto, TokenResponseDto>(
            path = SIGN_IN_PATH,
            body = SignInRequestDto(email = email.trim(), password = password),
            requiresAuth = false,
        ).persist()

    override suspend fun signUp(details: SignUpDetails): AppResult<Unit> =
        networkClient.post<SignUpRequestDto, TokenResponseDto>(
            path = SIGN_UP_PATH,
            body = SignUpRequestDto(
                name = details.name.trim(),
                email = details.email.trim(),
                password = details.password,
            ),
            requiresAuth = false,
        ).persist()

    /**
     * `execute` rather than `post<_, Unit>`: this endpoint conventionally answers 204 with no body,
     * and decoding an empty body into `Unit` fails at the JSON parse before it ever reaches the
     * `Unit`. Only the status matters here.
     */
    override suspend fun requestPasswordReset(email: String): AppResult<Unit> =
        networkClient.execute(
            NetworkRequest(
                method = HttpMethodType.POST,
                path = PASSWORD_RESET_PATH,
                body = NetworkJson.encodeToJsonElement(EmailRequestDto(email = email.trim())),
                requiresAuth = false,
            ),
        ).map { }

    /** Writes the tokens before returning success. */
    private suspend fun AppResult<TokenResponseDto>.persist(): AppResult<Unit> = when (this) {
        is AppResult.Success -> {
            tokenStore.save(
                accessToken = value.accessToken,
                refreshToken = value.refreshToken,
                expiresAtEpochMillis = value.expiresIn?.let {
                    (clock.now() + it.seconds).toEpochMilliseconds()
                },
            )
            map { }
        }

        is AppResult.Failure -> this
    }

    private companion object {
        // Change these to your backend's routes.
        const val SIGN_IN_PATH = "auth/login"
        const val SIGN_UP_PATH = "auth/register"
        const val PASSWORD_RESET_PATH = "auth/password/forgot"
    }
}
