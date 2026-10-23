package com.base.app.data.auth.api

import com.base.app.core.common.result.AppResult
import kotlinx.coroutines.flow.Flow

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
