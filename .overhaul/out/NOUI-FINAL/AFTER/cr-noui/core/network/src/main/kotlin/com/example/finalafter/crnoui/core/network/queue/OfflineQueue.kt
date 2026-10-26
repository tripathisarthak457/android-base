package com.example.finalafter.crnoui.core.network.queue

import com.example.finalafter.crnoui.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui.core.common.time.AppClock
import com.example.finalafter.crnoui.core.database.queue.QueuedRequestDao
import com.example.finalafter.crnoui.core.database.queue.QueuedRequestEntity
import com.example.finalafter.crnoui.core.datastore.session.AuthTokenStore
import com.example.finalafter.crnoui.core.network.client.NetworkJson
import com.example.finalafter.crnoui.core.network.model.NetworkRequest
import com.example.finalafter.crnoui.core.network.model.RequestBody
import io.ktor.http.encodeURLParameter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** A queued request as a screen of unsent work shows it. */
data class QueuedRequest(
    val id: Long,
    val method: String,
    val path: String,
    val createdAt: Instant,
    /** Null while it waits; the server's answer once it has been refused, or a bare refusal when it was given up on. */
    val refusal: Refusal?,
) {
    data class Refusal(val status: Int?, val code: String?, val message: String?, val at: Instant)
}

/**
 * The offline queue, stored in `queue.db`. Each entry belongs to whoever was signed in when it was
 * made and is sent only with their session, so a sign-out keeps it and the next person never sends
 * it under their own name.
 */
@Singleton
class OfflineQueue @Inject internal constructor(
    private val dao: QueuedRequestDao,
    private val tokenStore: AuthTokenStore,
    private val scheduler: ReplayScheduler,
    private val clock: AppClock,
    scope: ApplicationScope,
) : RequestQueue {

    init {
        // What someone queued before signing out waits for them. When they sign in again within
        // this process, nothing new may be queued to set a replay off, so the sign-in does.
        scope.launch {
            tokenStore.tokens.map { it?.owner }.distinctUntilChanged().drop(1).filterNotNull().collect { owner ->
                if (dao.sendable(owner).isNotEmpty()) scheduler.replaySoon()
            }
        }
    }

    override suspend fun enqueue(request: NetworkRequest) {
        dao.insert(
            QueuedRequestEntity(
                owner = if (request.requiresAuth) tokenStore.current()?.owner else null,
                method = request.method.name,
                path = request.pathWithQuery(),
                headersJson = NetworkJson.encodeToString(request.headers),
                body = (request.body as? RequestBody.Json)?.text,
                createdAtEpochMillis = clock.now().toEpochMilliseconds(),
            ),
        )
        scheduler.replaySoon()
    }

    /** What the signed-in person has queued, refused entries included, oldest first. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val entries: Flow<List<QueuedRequest>> = tokenStore.tokens
        .map { it?.owner }
        .distinctUntilChanged()
        .flatMapLatest(dao::observe)
        .map { rows -> rows.map { it.toQueuedRequest() } }

    /** Puts a refused entry back in line, once whatever the server objected to has been fixed. */
    suspend fun requeue(id: Long) {
        dao.requeue(id)
        scheduler.replaySoon()
    }

    suspend fun discard(id: Long) = dao.delete(id)
}

private fun QueuedRequestEntity.toQueuedRequest() = QueuedRequest(
    id = id,
    method = method,
    path = path,
    createdAt = Instant.fromEpochMilliseconds(createdAtEpochMillis),
    refusal = refusedAtEpochMillis?.let { at ->
        QueuedRequest.Refusal(refusedStatus, refusedCode, refusedMessage, Instant.fromEpochMilliseconds(at))
    },
)

/** The query goes into the stored path, since the table has no column of its own for it. */
private fun NetworkRequest.pathWithQuery(): String {
    val pairs = query.mapNotNull { (key, value) ->
        value?.let { "${key.encodeURLParameter()}=${it.toString().encodeURLParameter()}" }
    }
    return if (pairs.isEmpty()) path else path + "?" + pairs.joinToString("&")
}
