package com.example.finalafter.crui.core.common.session

/**
 * Anything holding data that belongs to the signed-in user and must not survive them signing out.
 */
fun interface SessionScopedStore {
    suspend fun clear()
}
