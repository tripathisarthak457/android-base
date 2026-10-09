package com.base.app.session

import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.log.Log
import com.base.app.core.common.session.SessionController
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.common.session.SessionState
import com.base.app.core.datastore.AuthTokenStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
// <opt:network>
import com.base.app.core.network.auth.SessionEvents
// </opt:network>

/**
 * Ends a session: clears every session-scoped store, then announces that it is safe to navigate.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val stores: Set<@JvmSuppressWildcards SessionScopedStore>,
    private val scope: ApplicationScope,
    tokenStore: AuthTokenStore,
    // <opt:network>
    private val sessionEvents: SessionEvents,
    // </opt:network>
) : SessionController {

    private val _state = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val state: StateFlow<SessionState> = _state.asStateFlow()

    private var generation = 0

    private val _signedOut = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits once teardown has finished. The navigation host resets the back stack on this. */
    val signedOut: Flow<Unit> = _signedOut.asSharedFlow()

    init {
        scope.launch {
            tokenStore.isAuthenticated.collect { authenticated ->
                if (authenticated && _state.value == SessionState.SignedOut) {
                    _state.value = SessionState.SignedIn(++generation)
                }
            }
        }
        // <opt:network>
        // An involuntary sign-out takes the same path as a deliberate one, so both clear the same
        // data.
        scope.launch {
            sessionEvents.expired.collect { signOut() }
        }
        // </opt:network>
    }

    override fun signOut() {
        scope.launch {
            stores.forEach { store ->
                // One store failing must not abandon the rest mid-wipe.
                runCatching { store.clear() }
                    .onFailure { Log.e("Failed clearing ${store::class.simpleName}", it) }
            }
            _state.value = SessionState.SignedOut
            _signedOut.tryEmit(Unit)
        }
    }
}
