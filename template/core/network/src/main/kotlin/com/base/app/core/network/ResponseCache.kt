package com.base.app.core.network

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.base.app.core.common.session.SessionScopedStore
import com.base.app.core.network.model.HttpMethodType
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responses kept on disk so a screen has something to show before — or instead of — a network
 * call.
 *
 * A separate concern from HTTP caching on purpose. OkHttp's cache obeys the server's
 * `Cache-Control`, and most internal APIs send none at all, or send `no-store` reflexively. This
 * is the client deciding, per call site, with a [com.base.app.core.network.model.CachePolicy].
 */
interface ResponseCache {

    /** A cached body younger than [maxAgeMillis], or null. */
    suspend fun fresh(key: String, maxAgeMillis: Long): NetworkResponse?

    /** A cached body of any age. The offline fallback. */
    suspend fun any(key: String): NetworkResponse?

    suspend fun put(key: String, body: String, statusCode: Int)
}

/**
 * Requests that failed for lack of connectivity, waiting to be replayed.
 *
 * Deliberately small in scope: JSON-bodied mutations only, replayed in the order they were made.
 * Anything more — conflict resolution, dependent requests, partial success — is a synchronisation
 * engine, and it needs to be designed around a specific domain rather than guessed at here.
 */
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

    @Query("SELECT * FROM queued_requests ORDER BY createdAtEpochMillis ASC")
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

    /**
     * Cleared on sign-out along with every other session-scoped store.
     *
     * Without this, the next person to sign in on a shared device is served the previous user's
     * cached responses — which is a data leak, not a stale-UI bug.
     */
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
                path = request.path,
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
