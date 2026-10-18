package com.example.hud.everything.core.datastore.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.hud.everything.core.datastore.crypto.ValueCipher
import com.example.hud.everything.core.datastore.di.SessionTokensDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** One session's tokens, read and written together so a refresh never pairs an old token with a new one. */
@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** When the access token stops working, if the server said. */
    val expiresAtEpochMillis: Long? = null,
    /**
     * Whose session this is: the account id the server named, or an id made up at sign-in when it
     * named none. Work queued offline is sent only with its owner's session, and data on the device
     * is wiped when the owner changes.
     */
    val owner: String,
)

/** Where the session's tokens live. */
interface AuthTokenStore {

    /** The tokens, or null when nobody is signed in or what is stored can no longer be decrypted. */
    val tokens: Flow<AuthTokens?>

    val isAuthenticated: Flow<Boolean>
        get() = tokens.map { it != null }.distinctUntilChanged()

    suspend fun current(): AuthTokens? = tokens.first()

    suspend fun save(tokens: AuthTokens)

    /**
     * Swaps [expected] for [replacement], or clears the store when [replacement] is null, only if the
     * stored tokens are still [expected]; returns whether it did. A refresh that finishes after a
     * sign-out, or after someone else signed in, finds different tokens and changes nothing.
     */
    suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean

    suspend fun clear()
}

/**
 * The tokens encrypted with a Keystore key, as one value in a file of their own. A key lost to a
 * device restore or an OEM Keystore bug leaves a value that cannot be decrypted, and that reads as
 * signed out rather than as a session no request can use.
 */
@Singleton
internal class DataStoreAuthTokenStore @Inject constructor(
    @SessionTokensDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: ValueCipher,
) : AuthTokenStore {

    override val tokens: Flow<AuthTokens?> = dataStore.data
        .map { preferences -> preferences[TOKENS]?.let(::decode) }
        .distinctUntilChanged()

    override suspend fun save(tokens: AuthTokens) {
        val sealed = cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), tokens))
        dataStore.edit { it[TOKENS] = sealed }
    }

    override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean {
        val sealed = replacement?.let { cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), it)) }
        var swapped = false
        dataStore.edit { preferences ->
            if (preferences[TOKENS]?.let(::decode) == expected) {
                if (sealed == null) preferences.remove(TOKENS) else preferences[TOKENS] = sealed
                swapped = true
            }
        }
        return swapped
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(TOKENS) }
    }

    private fun decode(sealed: String): AuthTokens? {
        val text = cipher.decrypt(sealed) ?: return null
        return try {
            Json.decodeFromString(AuthTokens.serializer(), text)
        } catch (unreadable: SerializationException) {
            null
        }
    }

    private companion object {
        val TOKENS = stringPreferencesKey("tokens")
    }
}
