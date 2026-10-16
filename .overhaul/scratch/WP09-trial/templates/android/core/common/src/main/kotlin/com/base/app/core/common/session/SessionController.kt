package com.base.app.core.common.session

import kotlinx.coroutines.flow.Flow

/** Ending the session, as seen by a feature. */
interface SessionController {

    val signedOut: Flow<Unit>

    fun signOut()
}
