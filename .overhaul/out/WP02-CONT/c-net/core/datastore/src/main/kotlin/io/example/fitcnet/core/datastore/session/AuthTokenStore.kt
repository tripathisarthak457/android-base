package io.example.fitcnet.core.datastore.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.example.fitcnet.core.common.coroutines.AppDispatchers
import io.example.fitcnet.core.datastore.crypto.ValueCipher
import io.example.fitcnet.core.datastore.di.SessionTokensDataStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

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

/**
 * The preferences file the session's tokens are kept in. The app's backup and device-transfer rules
 * leave it out by this name: the Keystore key that seals the tokens never leaves the device, so a
 * copy elsewhere is ciphertext nothing can read.
 */
const val SESSION_TOKENS_FILE = "session_tokens"

/** Where the session's tokens live. */
interface AuthTokenStore {

    /** The tokens, or null when nobody is signed in or what is stored can no longer be decrypted. */
    val tokens: Flow<AuthTokens?>

    /**
     * Whether a session is stored, which is what a launch decides between the signed-in and the
     * signed-out app on. A store that seals its tokens answers it without unsealing them, since the
     * first frame waits for it, and deletes tokens that turn out not to unseal when [tokens] reads
     * them, which turns this false.
     */
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
 * device restore or an OEM Keystore bug leaves a value that cannot be decrypted: the first read
 * that meets it deletes it, so it reads as signed out everywhere rather than as a session no
 * request can use.
 *
 * Sealing and unsealing are binder calls into the Keystore, so they run on [AppDispatchers.io]
 * whoever calls: a sign-in from a ViewModel would otherwise make them on the main thread.
 */
@Singleton
internal class DataStoreAuthTokenStore @Inject constructor(
    @SessionTokensDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: ValueCipher,
    private val dispatchers: AppDispatchers,
) : AuthTokenStore {

    override val tokens: Flow<AuthTokens?> = dataStore.data
        .map { preferences -> preferences[TOKENS]?.let { sealed -> decode(sealed) ?: discard(sealed) } }
        .distinctUntilChanged()
        .flowOn(dispatchers.io)

    override val isAuthenticated: Flow<Boolean> = dataStore.data
        .map { preferences -> TOKENS in preferences }
        .distinctUntilChanged()

    override suspend fun save(tokens: AuthTokens) {
        withContext(dispatchers.io) {
            val sealed = cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), tokens))
            dataStore.edit { it[TOKENS] = sealed }
        }
    }

    // DataStore runs an edit's transform in the caller's context, so the decode inside it needs
    // the switch as much as the encode before it.
    override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean =
        withContext(dispatchers.io) {
            val sealed = replacement?.let { cipher.encrypt(Json.encodeToString(AuthTokens.serializer(), it)) }
            var swapped = false
            dataStore.edit { preferences ->
                if (preferences[TOKENS]?.let(::decode) == expected) {
                    if (sealed == null) preferences.remove(TOKENS) else preferences[TOKENS] = sealed
                    swapped = true
                }
            }
            swapped
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

    /** Deletes [sealed] unless a save has replaced it since it was read. */
    private suspend fun discard(sealed: String): AuthTokens? {
        dataStore.edit { if (it[TOKENS] == sealed) it.remove(TOKENS) }
        return null
    }

    private companion object {
        val TOKENS = stringPreferencesKey("tokens")
    }
}
