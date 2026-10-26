package com.example.finalafter.crnoui.core.network.auth

import com.example.finalafter.crnoui.core.datastore.session.FakeAuthTokenStore
import com.example.finalafter.crnoui.core.network.tokens
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionTokensTest {

    private val store = FakeAuthTokenStore(tokens())
    private val refresher = ScriptedRefresher()
    private val events = SessionEvents()
    private val sessionTokens = SessionTokens(store, refresher, events)

    @Test
    fun `two refreshes at once present the refresh token once`() = runTest {
        refresher.gate = CompletableDeferred()
        val http = async { sessionTokens.refresh(stale = tokens()) }
        val socket = async { sessionTokens.refresh(stale = tokens()) }
        runCurrent()
        refresher.gate?.complete(Unit)

        assertEquals("A2", http.await()?.accessToken)
        assertEquals("A2", socket.await()?.accessToken)
        assertEquals(listOf("R1"), refresher.presented)
        assertEquals("R2", store.stored?.refreshToken)
    }

    @Test
    fun `a pair already replaced is answered from the store`() = runTest {
        store.save(tokens(access = "A2", refresh = "R2"))

        val renewed = sessionTokens.refresh(stale = tokens())

        assertEquals("A2", renewed?.accessToken)
        assertEquals(emptyList<String>(), refresher.presented)
    }

    @Test
    fun `someone else's session is not lent to a request made in the last one`() = runTest {
        store.save(tokens(access = "B1", refresh = "S1", owner = "grace"))

        val renewed = sessionTokens.refresh(stale = tokens())

        assertNull(renewed)
        assertEquals(emptyList<String>(), refresher.presented)
        assertEquals("grace", store.stored?.owner)
    }

    @Test
    fun `the renewed pair keeps its owner`() = runTest {
        assertEquals("ada", sessionTokens.refresh(stale = tokens())?.owner)
    }

    @Test
    fun `a rate-limited or unanswered refresh keeps the session`() = runTest {
        val expired = collectExpiry()

        refresher.answer = TokenRefreshResult.Failed(status = 429)
        sessionTokens.refresh(stale = tokens())
        refresher.answer = TokenRefreshResult.Failed(status = null)
        sessionTokens.refresh(stale = tokens())
        runCurrent()

        assertEquals("R1", store.stored?.refreshToken)
        assertEquals(0, expired.size)
    }

    @Test
    fun `a refused refresh token ends the session`() = runTest {
        refresher.answer = TokenRefreshResult.Refused
        val expired = collectExpiry()

        val renewed = sessionTokens.refresh(stale = tokens())
        runCurrent()

        assertNull(renewed)
        assertNull(store.stored)
        assertEquals(1, expired.size)
    }

    @Test
    fun `a refresh that finishes after sign-out does not bring the session back`() = runTest {
        refresher.gate = CompletableDeferred()
        val refreshing = async { sessionTokens.refresh(stale = tokens()) }
        runCurrent()
        store.clear()
        refresher.gate?.complete(Unit)

        assertNull(refreshing.await())
        assertNull(store.stored)
    }

    @Test
    fun `a sign-in during the refresh keeps the new person's tokens to themselves`() = runTest {
        refresher.gate = CompletableDeferred()
        val refreshing = async { sessionTokens.refresh(stale = tokens()) }
        runCurrent()
        store.save(tokens(access = "B1", refresh = "S1", owner = "grace"))
        refresher.gate?.complete(Unit)

        assertNull(refreshing.await())
        assertEquals("B1", store.stored?.accessToken)
    }

    @Test
    fun `a refusal that arrives after sign-in does not end the new session`() = runTest {
        refresher.answer = TokenRefreshResult.Refused
        refresher.gate = CompletableDeferred()
        val expired = collectExpiry()
        val refreshing = async { sessionTokens.refresh(stale = tokens()) }
        runCurrent()
        store.save(tokens(access = "B1", refresh = "S1", owner = "grace"))
        refresher.gate?.complete(Unit)
        refreshing.await()
        runCurrent()

        assertEquals("grace", store.stored?.owner)
        assertEquals(0, expired.size)
    }

    private fun TestScope.collectExpiry(): List<Unit> {
        val seen = mutableListOf<Unit>()
        backgroundScope.launch { events.expired.collect { seen += it } }
        runCurrent()
        return seen
    }

    private class ScriptedRefresher : TokenRefresher {
        val presented = mutableListOf<String>()
        var gate: CompletableDeferred<Unit>? = null
        var answer: TokenRefreshResult = TokenRefreshResult.Success("A2", "R2", expiresAtEpochMillis = null)

        override suspend fun refresh(refreshToken: String): TokenRefreshResult {
            presented += refreshToken
            gate?.await()
            return answer
        }
    }
}
