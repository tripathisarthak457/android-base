package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.core.common.result.map
import __PKG__.core.database.__NAME__.__P__Dao
import __PKG__.data.__NAME__.api.__P__Repository
import __PKG__.data.__NAME__.api.model.__P__Item
import __PKG__.data.__NAME__.local.toDomain
import __PKG__.data.__NAME__.local.toEntity
import __PKG__.data.__NAME__.remote.__P__Api
import __PKG__.data.__NAME__.remote.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The database is the source of truth: screens read [items] from it, and [refresh] writes the
 * server's answer into it. What was fetched last is on screen at once, offline included, and the
 * list follows every write.
 *
 * Every reader queries the table itself rather than sharing a cached copy, so a list read after
 * [refresh] returns is the one it wrote. A shared copy is only updated once the database has told
 * its observer about the write, and until then it hands back the list from before.
 */
@Singleton
internal class OfflineFirst__P__Repository @Inject constructor(
    private val api: __P__Api,
    private val dao: __P__Dao,
) : __P__Repository {

    private val readLock = Any()
    private var readers = 0

    // What the readers of [items] saw last, for [peek]. It goes with the last reader, so rows the
    // database has dropped since (a sign-out empties it) are never answered from memory.
    private var lastRead: List<__P__Item>? = null

    override val items: Flow<List<__P__Item>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .onStart { synchronized(readLock) { readers++ } }
        .onEach { read -> synchronized(readLock) { lastRead = read } }
        .onCompletion { synchronized(readLock) { if (--readers == 0) lastRead = null } }

    override suspend fun refresh(): AppResult<Unit> = when (val fetched = api.items()) {
        is AppResult.Success -> storing {
            dao.replaceAll(fetched.value.mapIndexed { position, dto -> dto.toEntity(position) })
        }

        is AppResult.Failure -> fetched
    }

    override fun peek(id: String): __P__Item? =
        synchronized(readLock) { lastRead }?.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<__P__Item> =
        dao.byId(id)?.let { AppResult.Success(it.toDomain()) } ?: api.item(id).map { it.toDomain() }

    /** A full disk or a damaged file is a failure to report, not a crash at whoever asked. */
    private suspend fun storing(write: suspend () -> Unit): AppResult<Unit> = try {
        write()
        AppResult.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        AppResult.Failure(AppError.Storage(failure))
    }
}
