package com.example.finalafter.crui.core.network.client

import com.example.finalafter.crui.core.common.coroutines.ApplicationScope
import com.example.finalafter.crui.core.common.result.AppResult
import com.example.finalafter.crui.core.common.session.SessionScopedStore
import com.example.finalafter.crui.core.network.model.NetworkResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Identical reads made while one is already on its way wait for that one instead of sending their
 * own. A screen and its detail asking for the same list on launch cost one request rather than two.
 *
 * The shared call runs in the application scope, so the caller that started it leaving the screen
 * does not cancel it for everyone else waiting on it. It belongs to the session: a read started just
 * before sign-out is never handed to the next person's identical read.
 */
@Singleton
class InFlightRequests @Inject constructor(
    private val scope: ApplicationScope,
) : SessionScopedStore {

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
                    mutex.withLock { if (running[key] === coroutineContext[Job]) running.remove(key) }
                }
            }.also { running[key] = it }
        }
        shared.start()
        return shared.await()
    }

    override suspend fun clear() {
        val dropped = mutex.withLock { running.values.toList().also { running.clear() } }
        dropped.forEach { it.cancel() }
    }
}
