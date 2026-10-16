package com.base.app.core.network

import com.base.app.core.common.AppResult
import com.base.app.core.coroutines.ApplicationScope
import com.base.app.core.network.model.NetworkResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identical reads made while one is already on its way wait for that one instead of sending their
 * own. A screen and a widget refreshing the same list, or a list recomposing twice on launch, cost
 * one request rather than several.
 *
 * The shared call runs in the application scope, so the caller that started it leaving the screen
 * does not cancel it for everyone else waiting on it.
 */
@Singleton
class InFlightRequests @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val running = mutableMapOf<String, Deferred<AppResult<NetworkResponse>>>()

    suspend fun shareOrRun(
        key: String,
        call: suspend () -> AppResult<NetworkResponse>,
    ): AppResult<NetworkResponse> {
        val shared = mutex.withLock {
            running[key] ?: scope.async(start = CoroutineStart.LAZY) {
                try {
                    call()
                } finally {
                    mutex.withLock { running.remove(key) }
                }
            }.also { running[key] = it }
        }
        shared.start()
        return shared.await()
    }
}
