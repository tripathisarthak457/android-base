package com.base.app.core.network.queue

import com.base.app.core.common.log.Log
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.time.AppClock
import com.base.app.core.database.queue.QueuedRequestDao
import com.base.app.core.database.queue.QueuedRequestEntity
import com.base.app.core.datastore.session.AuthTokenStore
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.NetworkJson
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.RequestBody
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Sends what the offline queue holds, oldest first, for the person signed in now.
 *
 * A request the server refuses is kept, marked with the server's answer, and the pass moves on:
 * sending it again cannot change the answer, and whoever made it has to be able to see that it was
 * not accepted. No answer, a 5xx, or a refusal about the moment rather than the request (an expired
 * session, a timeout, an outdated app, a rate limit) ends the pass and keeps the rest in order for
 * the next one. An entry that has waited [MAX_AGE] or met [MAX_ATTEMPTS] such failures is given up
 * on, and kept the same way.
 */
@Singleton
class QueuedRequestReplayer @Inject internal constructor(
    private val dao: QueuedRequestDao,
    private val client: NetworkClient,
    private val tokenStore: AuthTokenStore,
    private val clock: AppClock,
) {
    /** One pass at a time, or two could send the same entry. */
    private val passes = Mutex()

    /** One pass. Returns how many entries are still waiting for the next one. */
    suspend fun replayPending(): Int = passes.withLock {
        val waiting = dao.sendable(tokenStore.current()?.owner)
        waiting.forEachIndexed { index, entry ->
            val now = clock.now()
            if (now - Instant.fromEpochMilliseconds(entry.createdAtEpochMillis) > MAX_AGE ||
                entry.attempts >= MAX_ATTEMPTS
            ) {
                Log.w("Gave up on queued ${entry.method} ${entry.path}", tag = TAG)
                dao.markRefused(entry.id, now.toEpochMilliseconds(), status = null, code = null, message = null)
                return@forEachIndexed
            }
            when (val result = client.execute(entry.toRequest())) {
                is AppResult.Success -> dao.delete(entry.id)
                is AppResult.Failure -> {
                    val refusal = result.error as? AppError.Http
                    if (refusal != null && refusal.refusesEntry()) {
                        Log.w("Server refused queued ${entry.method} ${entry.path}: ${refusal.status}", tag = TAG)
                        dao.markRefused(entry.id, now.toEpochMilliseconds(), refusal.status, refusal.code, refusal.message)
                    } else {
                        dao.countAttempt(entry.id)
                        return@withLock waiting.size - index
                    }
                }
            }
        }
        0
    }

    private fun QueuedRequestEntity.toRequest() = NetworkRequest(
        method = HttpMethodType.valueOf(method),
        path = path,
        headers = NetworkJson.decodeFromString<Map<String, String>>(headersJson),
        body = body?.let(RequestBody::Json),
        requiresAuth = owner != null,
    )

    private companion object {
        const val TAG = "OfflineQueue"
        const val MAX_ATTEMPTS = 20
        val MAX_AGE = 7.days
    }
}

/** A 4xx about the request itself, rather than about the moment it was sent. */
private fun AppError.Http.refusesEntry(): Boolean = status in REFUSED_RANGE && status !in MOMENTARY_REFUSALS

private val REFUSED_RANGE = 400..499
private val MOMENTARY_REFUSALS = setOf(401, 408, 426, 429)
