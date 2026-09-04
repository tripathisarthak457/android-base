package com.base.app.session

import com.base.app.core.common.session.SessionController
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.common.util.AppLogger
import com.base.app.core.coroutines.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
// <opt:network>
import com.base.app.core.network.auth.SessionEvents
// </opt:network>

/**
 * Ends a session: clears every session-scoped store, then announces that it is safe to navigate.
 *
 * ## The order is load-bearing
 *
 * Teardown completes *before* [signedOut] emits. Navigating first and wiping afterwards leaves a
 * window in which the sign-in screen is on top while the previous user's cached responses are
 * still on disk — and if the process is killed in that window, they stay there for the next
 * person to use the device.
 *
 * ## Nothing here names a store
 *
 * The set is multibound; a store registers itself next to its own binding. A hand-written list of
 * things to clear is a list somebody forgets to update, and the symptom is a data leak rather
 * than a crash, so nothing surfaces it.
 *
 * ## It runs in the application scope
 *
 * Sign-out must finish even though the screen that triggered it is being destroyed at the same
 * moment. A `viewModelScope` here would cancel the wipe halfway through.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val stores: Set<@JvmSuppressWildcards SessionScopedStore>,
    @ApplicationScope private val scope: CoroutineScope,
    // <opt:network>
    private val sessionEvents: SessionEvents,
    // </opt:network>
) : SessionController {

    private val _signedOut = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits once teardown has finished. The navigation host resets the back stack on this. */
    override val signedOut: Flow<Unit> = _signedOut.asSharedFlow()

    init {
        // <opt:network>
        // An involuntary sign-out — the server refused the refresh token — takes exactly the same
        // path as a deliberate one. Two paths would eventually differ, and the one nobody tests
        // is the one that leaves data behind.
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
                    .onFailure { AppLogger.e("Failed clearing ${store::class.simpleName}", it) }
            }
            _signedOut.tryEmit(Unit)
        }
    }
}
