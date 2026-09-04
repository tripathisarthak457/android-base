package com.base.app.core.common.session

import kotlinx.coroutines.flow.Flow

/**
 * Ending the session, as seen by a feature.
 *
 * The implementation lives in `:app`, because ending a session means clearing every
 * [SessionScopedStore] and resetting navigation — both of which are composition-root concerns. But
 * the *button* lives in a settings screen, and a feature module cannot depend on `:app`.
 *
 * So the interface sits at the bottom of the graph and the implementation is bound at the top,
 * which is the standard inversion and the reason `:core:*` never needs to know a feature exists.
 *
 * [signedOut] emits only once teardown has finished. Navigating away first leaves a window in
 * which the sign-in screen is showing while the previous user's cached data is still on disk.
 */
interface SessionController {

    val signedOut: Flow<Unit>

    fun signOut()
}
