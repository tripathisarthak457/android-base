package com.example.finalafter.crui.data.auth

import com.example.finalafter.crui.core.common.result.AppError
import com.example.finalafter.crui.core.common.result.AppResult
import com.example.finalafter.crui.core.common.result.map
import com.example.finalafter.crui.core.common.time.AppClock
import com.example.finalafter.crui.core.datastore.session.AuthTokenStore
import com.example.finalafter.crui.core.datastore.session.AuthTokens
import com.example.finalafter.crui.core.network.client.NetworkClient
import com.example.finalafter.crui.core.network.client.post
import com.example.finalafter.crui.data.auth.api.AuthRepository
import com.example.finalafter.crui.data.auth.api.SignUpDetails
import com.example.finalafter.crui.data.auth.remote.dto.EmailRequestDto
import com.example.finalafter.crui.data.auth.remote.dto.GoogleSignInRequestDto
import com.example.finalafter.crui.data.auth.remote.dto.SignInRequestDto
import com.example.finalafter.crui.data.auth.remote.dto.SignUpRequestDto
import com.example.finalafter.crui.data.auth.remote.dto.TokenResponseDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Talks to the auth endpoints and puts what comes back in the token store.
 *
 * Every call passes `requiresAuth = false`: with a token attached, a wrong password would trigger a
 * token refresh and surface as a session expiry instead.
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
            carriesCredentials = true,
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
            carriesCredentials = true,
        ).persist()

    /** Only the status matters: this endpoint conventionally answers 204 with no body. */
    override suspend fun requestPasswordReset(email: String): AppResult<Unit> =
        networkClient.post<EmailRequestDto, Unit>(
            path = PASSWORD_RESET_PATH,
            body = EmailRequestDto(email = email.trim()),
            requiresAuth = false,
        )

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
            carriesCredentials = true,
        ).persist()

    /** Stores the tokens before reporting success, so the shell that appears next finds a session. */
    private suspend fun AppResult<TokenResponseDto>.persist(): AppResult<Unit> = when (this) {
        is AppResult.Success -> try {
            tokenStore.save(
                AuthTokens(
                    accessToken = value.accessToken,
                    refreshToken = value.refreshToken,
                    expiresAtEpochMillis = value.expiresIn?.let { (clock.now() + it.seconds).toEpochMilliseconds() },
                    owner = value.userId ?: Uuid.random().toString(),
                ),
            )
            map { }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            // The Keystore or the disk refused the tokens: the sign-in did not stick, and saying so
            // is better than a session that vanishes on the next launch.
            AppResult.Failure(AppError.Storage(failure))
        }

        is AppResult.Failure -> this
    }

    private companion object {
        // Change these to your backend's routes.
        const val SIGN_IN_PATH = "auth/login"
        const val SIGN_UP_PATH = "auth/register"
        const val PASSWORD_RESET_PATH = "auth/password/forgot"
        const val GOOGLE_SIGN_IN_PATH = "auth/google"
    }
}
