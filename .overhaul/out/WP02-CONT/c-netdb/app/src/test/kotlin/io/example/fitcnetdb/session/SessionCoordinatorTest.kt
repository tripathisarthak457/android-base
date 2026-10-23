package io.example.fitcnetdb.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import io.example.fitcnetdb.core.common.coroutines.ApplicationScope
import io.example.fitcnetdb.core.common.session.SessionScopedStore
import io.example.fitcnetdb.core.common.session.SessionState
import io.example.fitcnetdb.core.datastore.session.AuthTokenStore
import io.example.fitcnetdb.core.datastore.session.AuthTokens
import io.example.fitcnetdb.core.network.auth.SessionEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCoordinatorTest {

    private val tokens = FakeTokenStore()
    private var clears = 0
    private val store = SessionScopedStore { clears++ }

    @Test
    fun `two sign-outs at once clear every store exactly once`() = runTest {
        val session = coordinator()
        tokens.save(tokensFor("ada"))
        runCurrent()
        assertEquals(SessionState.SignedIn(1), session.state.value)

        session.signOut()
        session.signOut()
        runCurrent()

        assertEquals(1, clears)
        assertEquals(SessionState.SignedOut, session.state.value)
        assertNull(tokens.current())
    }

    @Test
    fun `someone else signing in over a session that never signed out finds nothing of it`() = runTest {
        val session = coordinator()
        tokens.save(tokensFor("ada"))
        runCurrent()

        tokens.save(tokensFor("grace"))
        runCurrent()

        assertEquals(1, clears)
        assertEquals(SessionState.SignedIn(2), session.state.value)
    }

    @Test
    fun `a renewed session for the same person keeps their data`() = runTest {
        val session = coordinator()
        tokens.save(tokensFor("ada"))
        runCurrent()

        tokens.save(tokensFor("ada").copy(accessToken = "renewed"))
        runCurrent()

        assertEquals(0, clears)
        assertEquals(SessionState.SignedIn(1), session.state.value)
    }

    @Test
    fun `tokens that cannot be read at start-up clear what the last person left`() = runTest {
        val owners = MemoryPreferences()
        owners.edit { it[stringPreferencesKey("owner")] = "ada" }

        coordinator(owners)
        runCurrent()

        assertEquals(1, clears)
    }

    private fun TestScope.coordinator(owners: DataStore<Preferences> = MemoryPreferences()) =
        SessionCoordinator(
            stores = setOf(store),
            tokenStore = tokens,
            ownerStore = owners,
            scope = ApplicationScope(backgroundScope),
            sessionEvents = SessionEvents(),
        ).also { runCurrent() }

    private fun tokensFor(owner: String) = AuthTokens(accessToken = "a", refreshToken = "r", owner = owner)
}

private class FakeTokenStore : AuthTokenStore {
    private val stored = MutableStateFlow<AuthTokens?>(null)

    override val tokens: Flow<AuthTokens?> = stored

    override suspend fun current(): AuthTokens? = stored.first()

    override suspend fun save(tokens: AuthTokens) {
        stored.value = tokens
    }

    override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean {
        if (stored.value != expected) return false
        stored.value = replacement
        return true
    }

    override suspend fun clear() {
        stored.value = null
    }
}

private class MemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
