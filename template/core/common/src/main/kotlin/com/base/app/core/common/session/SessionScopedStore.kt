package com.base.app.core.common.session

/**
 * Anything holding data that belongs to the signed-in user and must not survive them signing out.
 *
 * Implementations register themselves into a Hilt `@IntoSet`, and the sign-out path clears every
 * member of that set. The alternative — a `clearEverything()` function that names each store —
 * is a function somebody forgets to update, and the symptom is the next user of a shared device
 * seeing the previous one's cached data. Registration lives next to the store it clears, so
 * adding a store and forgetting to wipe it is not possible.
 *
 * [clear] must be idempotent and must not throw: it runs during teardown, where a failure would
 * abandon the remaining stores mid-wipe.
 */
interface SessionScopedStore {
    suspend fun clear()
}
