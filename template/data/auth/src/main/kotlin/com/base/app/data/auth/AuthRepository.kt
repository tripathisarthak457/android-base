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
// <opt:googlesignin>
import com.base.app.data.auth.remote.GoogleSignInRequestDto
// </opt:googlesignin>
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

/** Signing in, signing up, and knowing whether either has happened. */
interface AuthRepository {

    val isSignedIn: Flow<Boolean>

    suspend fun signIn(email: String, password: String): AppResult<Unit>

    suspend fun signUp(details: SignUpDetails): AppResult<Unit>

    suspend fun requestPasswordReset(email: String): AppResult<Unit>
    // <opt:googlesignin>

    /** Trades an ID token from Google for this app's own tokens. */
    suspend fun signInWithGoogle(idToken: String): AppResult<Unit>
    // </opt:googlesignin>
}

/**
 * Talks to the auth endpoints and puts what comes back in the token store.
 *
 * Every call passes `requiresAuth = false`: with a bearer token attached, a wrong password would
 * trigger a token refresh and surface as a session expiry instead.
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

    // <opt:googlesignin>
    /**
     * The backend verifies the ID token with Google and answers with tokens of its own, exactly as
     * a password sign-in does. The app never treats Google's token as a session: it expires in an
     * hour and says nothing about what this backend allows.
     */
    override suspend fun signInWithGoogle(idToken: String): AppResult<Unit> =
        networkClient.post<GoogleSignInRequestDto, TokenResponseDto>(
            path = GOOGLE_SIGN_IN_PATH,
            body = GoogleSignInRequestDto(idToken = idToken),
            requiresAuth = false,
        ).persist()

    // </opt:googlesignin>
    /** Writes the tokens before returning success. */
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
     */
    private fun AppResult.Failure.withFriendlyMessage(): AppResult.Failure = when (code) {
        HTTP_UNAUTHORIZED -> copy(message = "That email and password do not match an account.")
        HTTP_TOO_MANY_REQUESTS -> copy(message = "Too many attempts. Try again in a few minutes.")
        else -> this
    }

    private companion object {
        // Change these to your backend's routes.
        const val SIGN_IN_PATH = "auth/login"
        const val SIGN_UP_PATH = "auth/register"
        const val PASSWORD_RESET_PATH = "auth/password/forgot"
        // <opt:googlesignin>
        const val GOOGLE_SIGN_IN_PATH = "auth/google"
        // </opt:googlesignin>
        const val MILLIS_PER_SECOND = 1_000L
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_TOO_MANY_REQUESTS = 429
    }
}
