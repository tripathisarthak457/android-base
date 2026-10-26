package com.example.finalafter.crnoui.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.finalafter.crnoui.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui.core.common.session.SessionScopedStore
import com.example.finalafter.crnoui.core.common.session.SessionState
import com.example.finalafter.crnoui.core.datastore.FakePreferencesDataStore
import com.example.finalafter.crnoui.core.datastore.session.AuthTokens
import com.example.finalafter.crnoui.core.datastore.session.FakeAuthTokenStore
import com.example.finalafter.crnoui.core.network.auth.SessionEvents
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionCoordinatorTest {

    private val tokens = FakeAuthTokenStore()
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
        val owners = FakePreferencesDataStore()
        owners.edit { it[stringPreferencesKey("owner")] = "ada" }

        coordinator(owners)
        runCurrent()

        assertEquals(1, clears)
    }

    private fun TestScope.coordinator(owners: DataStore<Preferences> = FakePreferencesDataStore()) =
        SessionCoordinator(
            stores = setOf(store),
            tokenStore = tokens,
            ownerStore = owners,
            scope = ApplicationScope(backgroundScope),
            sessionEvents = SessionEvents(),
        ).also { runCurrent() }

    private fun tokensFor(owner: String) = AuthTokens(accessToken = "a", refreshToken = "r", owner = owner)
}
