package io.example.audit.core.network

import io.example.audit.core.datastore.session.AuthTokenStore
import io.example.audit.core.datastore.session.AuthTokens
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** An [AuthTokenStore] in memory, for tests that need a session without a Keystore. */
internal class MemoryTokenStore(initial: AuthTokens? = null) : AuthTokenStore {

    private val state = MutableStateFlow(initial)

    val stored: AuthTokens? get() = state.value

    override val tokens: Flow<AuthTokens?> = state

    override suspend fun save(tokens: AuthTokens) {
        state.value = tokens
    }

    override suspend fun replace(expected: AuthTokens, replacement: AuthTokens?): Boolean {
        if (state.value != expected) return false
        state.value = replacement
        return true
    }

    override suspend fun clear() {
        state.value = null
    }
}

internal fun tokens(access: String = "A1", refresh: String = "R1", owner: String = "ada") =
    AuthTokens(accessToken = access, refreshToken = refresh, owner = owner)
