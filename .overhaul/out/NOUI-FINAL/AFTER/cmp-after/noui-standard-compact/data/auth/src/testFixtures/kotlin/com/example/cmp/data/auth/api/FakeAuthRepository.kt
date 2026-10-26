package com.example.cmp.data.auth.api

import com.example.cmp.core.common.result.AppResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An [AuthRepository] whose server answers every request with [result], recording what each
 * request sent. While [gate] is set, every answer waits for it, so a test can act while a request
 * is in flight. While [thrown] is set, every request throws it instead, as a repository with a bug
 * would.
 */
class FakeAuthRepository : AuthRepository {

    var result: AppResult<Unit> = AppResult.Success(Unit)
    var gate: CompletableDeferred<Unit>? = null
    var thrown: Throwable? = null

    /** Each sign-in, as the email and password it sent. */
    val signIns = mutableListOf<Pair<String, String>>()
    val signUps = mutableListOf<SignUpDetails>()

    /** The address of each password-reset request. */
    val resetRequests = mutableListOf<String>()

    override val isSignedIn = MutableStateFlow(false)

    override suspend fun signIn(email: String, password: String): AppResult<Unit> {
        signIns += email to password
        return answer()
    }

    override suspend fun signUp(details: SignUpDetails): AppResult<Unit> {
        signUps += details
        return answer()
    }

    override suspend fun requestPasswordReset(email: String): AppResult<Unit> {
        resetRequests += email
        return answer()
    }

    private suspend fun answer(): AppResult<Unit> {
        gate?.await()
        thrown?.let { throw it }
        return result
    }
}
