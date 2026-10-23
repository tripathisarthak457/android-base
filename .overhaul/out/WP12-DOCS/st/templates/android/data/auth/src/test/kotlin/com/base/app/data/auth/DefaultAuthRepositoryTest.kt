package com.base.app.data.auth

import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.time.AppClock
import com.base.app.core.datastore.session.AuthTokenStore
import com.base.app.core.datastore.session.AuthTokens
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.NetworkJson
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

class DefaultAuthRepositoryTest {

    private val store = MemoryTokenStore()
    private val client = ScriptedClient()
    private val repository = DefaultAuthRepository(client, store, AppClock { Instant.fromEpochSeconds(1_000) })

    @Test
    fun `a sign-in stores the tokens with the account the server named`() = runTest {
        client.answer = """{"access_token":"A1","refresh_token":"R1","expires_in":60,"user_id":"u42"}"""

        val result = repository.signIn("ada@example.com", "secret")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(AuthTokens("A1", "R1", expiresAtEpochMillis = 1_060_000, owner = "u42"), store.saved)
        assertTrue(client.requests.single().carriesCredentials)
    }

    @Test
    fun `a sign-in without an account id still gets an owner of its own`() = runTest {
        client.answer = """{"access_token":"A1","refresh_token":"R1"}"""

        repository.signIn("ada@example.com", "secret")

        assertTrue(store.saved?.owner.orEmpty().isNotBlank())
    }

    @Test
    fun `tokens the store cannot keep fail the sign-in as storage`() = runTest {
        client.answer = """{"access_token":"A1","refresh_token":"R1"}"""
        store.refuse = true

        val result = repository.signIn("ada@example.com", "secret")

        assertTrue((result as AppResult.Failure).error is AppError.Storage)
    }

    @Test
    fun `a refusal passes through and stores nothing`() = runTest {
        client.failure = AppError.Http(status = 401)

        val result = repository.signIn("ada@example.com", "wrong")

        assertEquals(AppResult.Failure(AppError.Http(status = 401)), result)
        assertNull(store.saved)
    }

    private class ScriptedClient : NetworkClient {
        var answer = "{}"
        var failure: AppError? = null
        val requests = mutableListOf<NetworkRequest>()

        override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
            requests += request
            return failure?.let { AppResult.Failure(it) } ?: AppResult.Success(NetworkResponse(200, answer))
        }

        override suspend fun <T> execute(request: NetworkRequest, deserializer: DeserializationStrategy<T>): AppResult<T> =
            when (val result = execute(request)) {
                is AppResult.Success -> AppResult.Success(NetworkJson.decodeFromString(deserializer, result.value.body))
                is AppResult.Failure -> result
            }
    }

    private class MemoryTokenStore : AuthTokenStore {
        private val state = MutableStateFlow<AuthTokens?>(null)
        var refuse = false
        val saved: AuthTokens? get() = state.value

        override val tokens: Flow<AuthTokens?> = state

        override suspend fun save(tokens: AuthTokens) {
            check(!refuse) { "Keystore unavailable" }
            state.value = tokens
        }

        override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean = false

        override suspend fun clear() {
            state.value = null
        }
    }
}
