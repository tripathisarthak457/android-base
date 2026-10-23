package com.example.none.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.none.core.common.coroutines.ApplicationScope
import com.example.none.core.common.log.Log
import com.example.none.core.common.session.SessionController
import com.example.none.core.common.session.SessionScopedStore
import com.example.none.core.common.session.SessionState
import com.example.none.core.datastore.session.AuthTokenStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps each person's data to that person on a shared phone. Every [SessionScopedStore] is emptied
 * when a session ends, whichever way it ends: a deliberate sign-out, the server refusing the session,
 * or tokens that can no longer be read. It is emptied too when the device's data belongs to someone
 * other than the person now signing in, which the owner kept across launches tells; signing back in
 * as the same person keeps it.
 *
 * [state] moves to a new [SessionState.SignedIn] generation with every new owner, which is what the
 * shell keys its tabs and stacks on.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val stores: Set<@JvmSuppressWildcards SessionScopedStore>,
    private val tokenStore: AuthTokenStore,
    @SessionOwnerStore private val ownerStore: DataStore<Preferences>,
    private val scope: ApplicationScope,
) : SessionController {

    private val _state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    /** One change at a time: two sign-outs at once clear each store once, not twice. */
    private val changes = Mutex()
    private var generation = 0

    init {
        scope.launch {
            tokenStore.tokens.map { it?.owner }.distinctUntilChanged().collect(::onOwner)
        }
    }

    override fun signOut() {
        scope.launch { endSession() }
    }

    private suspend fun endSession() = changes.withLock {
        if (tokenStore.current() == null && dataOwner() == null) return@withLock
        clearStores()
        tokenStore.clear()
        setDataOwner(null)
        _state.value = SessionState.SignedOut
    }

    private suspend fun onOwner(next: String?) = changes.withLock {
        val previous = dataOwner()
        if (next != previous) {
            if (previous != null) clearStores()
            setDataOwner(next)
        }
        when {
            next == null -> _state.value = SessionState.SignedOut

            next != previous || _state.value == SessionState.SignedOut -> _state.value = SessionState.SignedIn(++generation)
        }
    }

    private suspend fun clearStores() {
        stores.forEach { store ->
            // One store failing must not leave the rest holding the last person's data.
            try {
                store.clear()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Exception) {
                Log.e("Could not clear ${store::class.java.simpleName}", failure)
            }
        }
    }

    private suspend fun dataOwner(): String? = ownerStore.data.first()[OWNER]

    private suspend fun setDataOwner(owner: String?) {
        ownerStore.edit { if (owner == null) it.remove(OWNER) else it[OWNER] = owner }
    }

    private companion object {
        val OWNER = stringPreferencesKey("owner")
    }
}
