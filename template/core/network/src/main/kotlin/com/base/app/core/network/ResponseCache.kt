package com.base.app.core.network

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.base.app.core.common.AppResult
import com.base.app.core.common.network.NetworkMonitor
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.common.util.AppLogger
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responses kept on disk so a screen has something to show before — or instead of — a network call.
 */
interface ResponseCache {

    /** A cached body younger than [maxAgeMillis], or null. */
    suspend fun fresh(key: String, maxAgeMillis: Long): NetworkResponse?

    /** A cached body of any age. The offline fallback. */
    suspend fun any(key: String): NetworkResponse?

    suspend fun put(key: String, body: String, statusCode: Int)
}

/** Requests that failed for lack of connectivity, waiting to be replayed. */
interface RequestQueue {

    suspend fun enqueue(request: NetworkRequest)

    suspend fun pending(): List<QueuedRequest>

    suspend fun remove(id: Long)

    suspend fun clear()
}

@Serializable
data class QueuedRequest(
    val id: Long,
    val method: String,
    val path: String,
    val bodyJson: String?,
    val headersJson: String,
    val createdAtEpochMillis: Long,
)

@Entity(tableName = "cached_responses")
data class CachedResponseEntity(
    @PrimaryKey val cacheKey: String,
    val body: String,
    val statusCode: Int,
    val cachedAtEpochMillis: Long,
)

@Entity(tableName = "queued_requests")
data class QueuedRequestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val method: String,
    val path: String,
    val bodyJson: String?,
    val headersJson: String,
    val createdAtEpochMillis: Long,
)

@Dao
interface CachedResponseDao {

    @Query("SELECT * FROM cached_responses WHERE cacheKey = :key LIMIT 1")
    suspend fun find(key: String): CachedResponseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedResponseEntity)

    @Query("DELETE FROM cached_responses")
    suspend fun clear()
}

@Dao
interface QueuedRequestDao {

    @Query("SELECT * FROM queued_requests ORDER BY createdAtEpochMillis ASC, id ASC")
    suspend fun all(): List<QueuedRequestEntity>

    @Insert
    suspend fun insert(entity: QueuedRequestEntity)

    @Query("DELETE FROM queued_requests WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM queued_requests")
    suspend fun clear()
}

@Database(
    entities = [CachedResponseEntity::class, QueuedRequestEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NetworkDatabase : RoomDatabase() {
    abstract fun cachedResponseDao(): CachedResponseDao
    abstract fun queuedRequestDao(): QueuedRequestDao

    companion object {
        const val NAME = "network"
    }
}

@Singleton
class RoomResponseCache @Inject constructor(
    private val dao: CachedResponseDao,
) : ResponseCache, SessionScopedStore {

    override suspend fun fresh(key: String, maxAgeMillis: Long): NetworkResponse? {
        val entity = dao.find(key) ?: return null
        val age = System.currentTimeMillis() - entity.cachedAtEpochMillis
        return if (age <= maxAgeMillis) entity.toResponse() else null
    }

    override suspend fun any(key: String): NetworkResponse? = dao.find(key)?.toResponse()

    override suspend fun put(key: String, body: String, statusCode: Int) {
        dao.upsert(
            CachedResponseEntity(
                cacheKey = key,
                body = body,
                statusCode = statusCode,
                cachedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    /** Cleared on sign-out along with every other session-scoped store. */
    override suspend fun clear() {
        runCatching { dao.clear() }
    }

    private fun CachedResponseEntity.toResponse() = NetworkResponse(
        statusCode = statusCode,
        body = body,
        fromCache = true,
        cachedAtEpochMillis = cachedAtEpochMillis,
    )
}

@Singleton
class RoomRequestQueue @Inject constructor(
    private val dao: QueuedRequestDao,
) : RequestQueue, SessionScopedStore {

    override suspend fun enqueue(request: NetworkRequest) {
        if (request.method == HttpMethodType.GET) return
        dao.insert(
            QueuedRequestEntity(
                method = request.method.name,
                path = request.pathWithQuery(),
                bodyJson = request.body?.toString(),
                headersJson = NetworkJson.encodeToString(request.headers),
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun pending(): List<QueuedRequest> = dao.all().map { entity ->
        QueuedRequest(
            id = entity.id,
            method = entity.method,
            path = entity.path,
            bodyJson = entity.bodyJson,
            headersJson = entity.headersJson,
            createdAtEpochMillis = entity.createdAtEpochMillis,
        )
    }

    override suspend fun remove(id: Long) {
        runCatching { dao.delete(id) }
    }

    override suspend fun clear() {
        runCatching { dao.clear() }
    }
}

/** The query goes into the stored path, since the queue has no column of its own for it. */
private fun NetworkRequest.pathWithQuery(): String {
    val pairs = query.mapNotNull { (key, value) ->
        value?.let { "${key.encodeURLParameter()}=${it.toString().encodeURLParameter()}" }
    }
    return if (pairs.isEmpty()) path else path + "?" + pairs.joinToString("&")
}

/**
 * Sends what [RequestQueue] holds, oldest first, each time the device comes back online. A request
 * the server answers with a 4xx is dropped, because sending it again cannot change the answer. Any
 * other failure ends the pass and keeps the rest in order for the next one.
 */
@Singleton
class QueuedRequestReplayer @Inject constructor(
    private val queue: RequestQueue,
    private val client: NetworkClient,
    private val networkMonitor: NetworkMonitor,
) {

    /** Suspends for as long as the calling scope lives. Start it once, in the application scope. */
    suspend fun replayWhenOnline() {
        networkMonitor.isOnline.distinctUntilChanged().filter { it }.collect { replayPending() }
    }

    suspend fun replayPending() {
        for (queued in queue.pending()) {
            when (val result = client.execute(queued.toRequest())) {
                is AppResult.Success -> queue.remove(queued.id)
                is AppResult.Failure -> {
                    if (result.code !in REFUSED_RANGE) return
                    AppLogger.w("Dropped queued ${queued.method} ${queued.path}: ${result.code}")
                    queue.remove(queued.id)
                }
            }
        }
    }

    private fun QueuedRequest.toRequest() = NetworkRequest(
        method = HttpMethodType.valueOf(method),
        path = path,
        headers = NetworkJson.decodeFromString<Map<String, String>>(headersJson),
        body = bodyJson?.let(NetworkJson::parseToJsonElement),
    )

    private companion object {
        val REFUSED_RANGE = 400..499
    }
}
