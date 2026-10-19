package com.example.hud.fixture.core.datastore.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hud.fixture.core.common.coroutines.AppDispatchers
import com.example.hud.fixture.core.datastore.crypto.ValueCipher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.coroutines.CoroutineContext

class AuthTokenStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val ada = AuthTokens(accessToken = "A1", refreshToken = "R1", expiresAtEpochMillis = 5, owner = "ada")
    private val grace = AuthTokens(accessToken = "B1", refreshToken = "S1", owner = "grace")

    @Test
    fun `saved tokens read back whole and say someone is signed in`() = runTest {
        val store = store()

        store.save(ada)

        assertEquals(ada, store.current())
        assertTrue(store.isAuthenticated.first())
    }

    @Test
    fun `whether someone is signed in is answered without unsealing the tokens`() = runTest {
        val cipher = PrefixCipher()
        val store = store(cipher)
        store.save(ada)

        assertTrue(store.isAuthenticated.first())
        assertEquals(0, cipher.decrypted)
    }

    @Test
    fun `tokens that cannot be decrypted read as signed out, and the first read deletes them`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "tokens.preferences_pb") }
        val store = DataStoreAuthTokenStore(dataStore, PrefixCipher(), dispatchers())

        dataStore.edit { it[stringPreferencesKey("tokens")] = "sealed with a key this device lost" }
        assertTrue(store.isAuthenticated.first())

        assertNull(store.current())
        assertFalse(store.isAuthenticated.first())
    }

    @Test
    fun `replace changes nothing once the tokens it expected are gone`() = runTest {
        val store = store()
        store.save(grace)

        val swapped = store.replace(expected = ada, replacement = ada.copy(accessToken = "A2"))

        assertFalse(swapped)
        assertEquals(grace, store.current())
    }

    @Test
    fun `replace with nothing signs out only the session it expected`() = runTest {
        val store = store()
        store.save(ada)

        assertTrue(store.replace(expected = ada, replacement = null))
        assertNull(store.current())
    }

    @Test
    fun `the cipher runs on the io dispatcher whoever calls`() = runTest {
        val io = MarkedDispatcher(StandardTestDispatcher(testScheduler))
        var uses = 0
        val cipher = PrefixCipher(
            onUse = {
                assertTrue("The Keystore was used outside the io dispatcher", io.isCurrent)
                uses++
            },
        )
        val store = store(cipher, dispatchers(io = io))

        store.save(ada)
        store.current()
        store.replace(expected = ada, replacement = grace)

        assertEquals(4, uses)
    }

    private fun TestScope.store(cipher: ValueCipher = PrefixCipher(), dispatchers: AppDispatchers = dispatchers()) =
        DataStoreAuthTokenStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "tokens.preferences_pb") },
            cipher,
            dispatchers,
        )

    private fun TestScope.dispatchers(io: CoroutineDispatcher = StandardTestDispatcher(testScheduler)): AppDispatchers {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return AppDispatchers(io = io, default = dispatcher, main = dispatcher)
    }

    /** Reversible, and refuses what it did not seal, as a Keystore cipher refuses another key's output. */
    private class PrefixCipher(private val onUse: () -> Unit = {}) : ValueCipher {
        var decrypted = 0
            private set

        override fun encrypt(plaintext: String): String {
            onUse()
            return PREFIX + plaintext
        }

        override fun decrypt(encoded: String): String? {
            onUse()
            decrypted++
            return encoded.removePrefix(PREFIX).takeIf { encoded.startsWith(PREFIX) }
        }

        private companion object {
            const val PREFIX = "sealed:"
        }
    }

    /** Runs its tasks on [delegate] and tells whether the calling thread is inside one of them. */
    private class MarkedDispatcher(private val delegate: CoroutineDispatcher) : CoroutineDispatcher() {
        private val inside = ThreadLocal.withInitial { false }

        val isCurrent: Boolean get() = inside.get()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            delegate.dispatch(context) {
                inside.set(true)
                try {
                    block.run()
                } finally {
                    inside.set(false)
                }
            }
        }
    }
}
