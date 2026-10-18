package com.example.hud.standard.core.datastore.session

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hud.standard.core.datastore.crypto.ValueCipher
import kotlinx.coroutines.flow.first
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
    fun `tokens that cannot be decrypted read as signed out`() = runTest {
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "tokens.preferences_pb") }
        val store = DataStoreAuthTokenStore(dataStore, PrefixCipher())

        dataStore.edit { it[stringPreferencesKey("tokens")] = "sealed with a key this device lost" }

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

    private fun TestScope.store() = DataStoreAuthTokenStore(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "tokens.preferences_pb") },
        PrefixCipher(),
    )

    /** Reversible, and refuses what it did not seal, as a Keystore cipher refuses another key's output. */
    private class PrefixCipher : ValueCipher {
        override fun encrypt(plaintext: String): String = PREFIX + plaintext

        override fun decrypt(encoded: String): String? = encoded.removePrefix(PREFIX).takeIf { encoded.startsWith(PREFIX) }

        private companion object {
            const val PREFIX = "sealed:"
        }
    }
}
