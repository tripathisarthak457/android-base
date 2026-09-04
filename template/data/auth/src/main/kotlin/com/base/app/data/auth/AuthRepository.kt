package com.base.app.data.auth

import com.base.app.core.common.AppResult
import com.base.app.core.common.map
import com.base.app.core.datastore.AuthTokenStore
import com.base.app.core.network.NetworkClient
import com.base.app.core.network.NetworkJson
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.post
import com.base.app.data.auth.remote.EmailRequestDto
import com.base.app.data.auth.remote.SignInRequestDto
import com.base.app.data.auth.remote.SignUpRequestDto
import com.base.app.data.auth.remote.TokenResponseDto
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** What the caller supplies to create an account. A parameter object, so the order cannot slip. */
data class SignUpDetails(
    val name: String,
    val email: String,
    val password: String,
)

/**
 * Signing in, signing up, and knowing whether either has happened.
 *
 * Signing *out* is deliberately absent: it belongs to `SessionController`, which clears every
 * session-scoped store rather than only this one. An auth repository that could sign out would be
 * a second way to end a session, and the one nobody remembers to update is the one that leaves
 * the previous user's cached data on disk.
 */
interface AuthRepository {

    val isSignedIn: Flow<Boolean>

    suspend fun signIn(email: String, password: String): AppResult<Unit>

    suspend fun signUp(details: SignUpDetails): AppResult<Unit>

    suspend fun requestPasswordReset(email: String): AppResult<Unit>
}

/**
 * Talks to the auth endpoints and puts what comes back in the token store.
 *
 * ## `requiresAuth = false`
 *
 * Set on every call here. Without it the client attaches the current bearer token and, on a 401,
 * tries to refresh it — so a wrong password would trigger a refresh attempt, fail that too, and
 * surface as a session expiry rather than as "wrong password".
 *
 * ## The paths
 *
 * Change [SIGN_IN_PATH] and its neighbours to match your backend. They are the only strings in
 * this module that are not the backend's own field names.
 */
@Singleton
class DefaultAuthRepository @Inject constructor(
    private val networkClient: NetworkClient,
    private val tokenStore: AuthTokenStore,
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
     * `execute` rather than `post<_, Unit>`: this endpoint conventionally answers 204 with no
     * body, and decoding an empty body into `Unit` fails at the JSON parse before it ever reaches
     * the `Unit`. Only the status matters here.
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

    /**
     * Writes the tokens before returning success.
     *
     * Returning first and saving in a `launch` is the version that reads more cleanly and is
     * wrong: the screen navigates to a signed-in destination whose first request fires before the
     * token is on disk, and the user is bounced straight back to sign-in once in every few runs.
     */
    private suspend fun AppResult<TokenResponseDto>.persist(): AppResult<Unit> = when (this) {
        is AppResult.Success -> {
            tokenStore.save(
                accessToken = data.accessToken,
                refreshToken = data.refreshToken,
                expiresAtEpochMillis = data.expiresIn?.let {
                    System.currentTimeMillis() + it * MILLIS_PER_SECOND
                },
            )
            map { }
        }

        is AppResult.Failure -> withFriendlyMessage()
    }

    /**
     * Turns the two failures a sign-in form actually produces into sentences a person can act on.
     *
     * Everything else keeps the server's own message: a backend that returns "That account is
     * locked. Contact support." says it better than any string that could be written here.
     */
    private fun AppResult.Failure.withFriendlyMessage(): AppResult.Failure = when (code) {
        HTTP_UNAUTHORIZED -> copy(message = "That email and password do not match an account.")
        HTTP_TOO_MANY_REQUESTS -> copy(message = "Too many attempts. Try again in a few minutes.")
        else -> this
    }

    private companion object {
        const val SIGN_IN_PATH = "auth/login"
        const val SIGN_UP_PATH = "auth/register"
        const val PASSWORD_RESET_PATH = "auth/password/forgot"
        const val MILLIS_PER_SECOND = 1_000L
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
