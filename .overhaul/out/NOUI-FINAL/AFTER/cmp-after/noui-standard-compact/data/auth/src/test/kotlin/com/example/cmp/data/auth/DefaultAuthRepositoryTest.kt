package com.example.cmp.data.auth

import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.common.time.AppClock
import com.example.cmp.core.datastore.session.AuthTokens
import com.example.cmp.core.datastore.session.FakeAuthTokenStore
import com.example.cmp.core.network.client.NetworkClient
import com.example.cmp.core.network.client.NetworkJson
import com.example.cmp.core.network.model.NetworkRequest
import com.example.cmp.core.network.model.NetworkResponse
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.DeserializationStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultAuthRepositoryTest {

    private val store = FakeAuthTokenStore()
    private val client = ScriptedClient()
    private val repository =
        DefaultAuthRepository(client, store, AppClock { Instant.fromEpochSeconds(1_000) })

    @Test
    fun `a sign-in stores the tokens with the account the server named`() = runTest {
        client.answer =
            """{"access_token":"A1","refresh_token":"R1","expires_in":60,"user_id":"u42"}"""

        val result = repository.signIn("ada@example.com", "secret")

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(
            AuthTokens("A1", "R1", expiresAtEpochMillis = 1_060_000, owner = "u42"),
            store.stored,
        )
        assertTrue(client.requests.single().carriesCredentials)
    }

    @Test
    fun `a sign-in without an account id still gets an owner of its own`() = runTest {
        client.answer = """{"access_token":"A1","refresh_token":"R1"}"""

        repository.signIn("ada@example.com", "secret")

        assertTrue(store.stored?.owner.orEmpty().isNotBlank())
    }

    @Test
    fun `the owner made up for an account without an id is a fresh random UUID each time`() =
        runTest {
            client.answer = """{"access_token":"A1","refresh_token":"R1"}"""

            repository.signIn("ada@example.com", "secret")
            val first = store.stored?.owner.orEmpty()
            repository.signIn("ada@example.com", "secret")
            val second = store.stored?.owner.orEmpty()

            assertTrue(first, RandomUuid.matches(first))
            assertTrue(second, RandomUuid.matches(second))
            assertNotEquals(first, second)
        }

    @Test
    fun `tokens the store cannot keep fail the sign-in as storage`() = runTest {
        client.answer = """{"access_token":"A1","refresh_token":"R1"}"""
        store.refuseSaves = true

        val result = repository.signIn("ada@example.com", "secret")

        assertTrue((result as AppResult.Failure).error is AppError.Storage)
    }

    @Test
    fun `a refusal passes through and stores nothing`() = runTest {
        client.failure = AppError.Http(status = 401)

        val result = repository.signIn("ada@example.com", "wrong")

        assertEquals(AppResult.Failure(AppError.Http(status = 401)), result)
        assertNull(store.stored)
    }

    private class ScriptedClient : NetworkClient {
        var answer = "{}"
        var failure: AppError? = null
        val requests = mutableListOf<NetworkRequest>()

        override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
            requests += request
            return failure?.let { AppResult.Failure(it) }
                ?: AppResult.Success(NetworkResponse(200, answer))
        }

        override suspend fun <T> execute(
            request: NetworkRequest,
            deserializer: DeserializationStrategy<T>,
        ): AppResult<T> = when (val result = execute(request)) {
            is AppResult.Success -> AppResult.Success(
                NetworkJson.decodeFromString(deserializer, result.value.body),
            )

            is AppResult.Failure -> result
        }
    }

    private companion object {
        /** A version 4 (random) UUID in its canonical lower-case form. */
        val RandomUuid =
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
