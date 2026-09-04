package com.base.app.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.datastore.crypto.KeystoreCipher
import com.base.app.core.datastore.di.SessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the session tokens live.
 *
 * An interface because `:core:network` needs to read tokens and must not depend on how they are
 * stored — and because a test of the refresh flow needs to hand the client a token store it
 * controls, without a Keystore or a filesystem.
 */
interface AuthTokenStore {

    val isAuthenticated: Flow<Boolean>

    suspend fun accessToken(): String?

    suspend fun refreshToken(): String?

    /** Epoch millis the access token stops being valid, when the server told us. */
    suspend fun accessTokenExpiryMillis(): Long?

    suspend fun save(accessToken: String, refreshToken: String, expiresAtEpochMillis: Long? = null)

    suspend fun clear()
}

/**
 * Tokens on disk, encrypted with a Keystore-held key.
 *
 * Registered as a [SessionScopedStore] so sign-out clears it without anything having to name it —
 * see that interface for why that matters.
 */
@Singleton
class DataStoreAuthTokenStore @Inject constructor(
    @SessionDataStore private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher,
) : AuthTokenStore, SessionScopedStore {

    override val isAuthenticated: Flow<Boolean> = dataStore.data
        .map { it.contains(ACCESS_TOKEN) && it.contains(REFRESH_TOKEN) }
        .distinctUntilChanged()

    override suspend fun accessToken(): String? = read(ACCESS_TOKEN)

    override suspend fun refreshToken(): String? = read(REFRESH_TOKEN)

    override suspend fun accessTokenExpiryMillis(): Long? = dataStore.data.first()[EXPIRES_AT]

    override suspend fun save(
        accessToken: String,
        refreshToken: String,
        expiresAtEpochMillis: Long?,
    ) {
        dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = cipher.encrypt(accessToken)
            preferences[REFRESH_TOKEN] = cipher.encrypt(refreshToken)
            if (expiresAtEpochMillis != null) {
                preferences[EXPIRES_AT] = expiresAtEpochMillis
            } else {
                preferences.remove(EXPIRES_AT)
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(EXPIRES_AT)
        }
    }

    private suspend fun read(key: Preferences.Key<String>): String? =
        dataStore.data.first()[key]?.let(cipher::decrypt)

    private companion object {
        val ACCESS_TOKEN = stringPreferencesKey("encrypted_access_token")
        val REFRESH_TOKEN = stringPreferencesKey("encrypted_refresh_token")
        val EXPIRES_AT = longPreferencesKey("access_token_expires_at")
    }
}
