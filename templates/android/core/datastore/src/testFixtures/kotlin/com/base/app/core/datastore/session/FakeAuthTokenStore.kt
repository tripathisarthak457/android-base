package com.base.app.core.datastore.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An [AuthTokenStore] in memory, for a test that needs a session without a Keystore. Signing in is
 * [save] and signing out is [clear]; `isAuthenticated` follows [tokens], as in the real store.
 */
class FakeAuthTokenStore(initial: AuthTokens? = null) : AuthTokenStore {

    private val state = MutableStateFlow(initial)

    /** What is stored now, read without suspending. */
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
