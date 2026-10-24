package com.example.deskstandard.data.sample

import com.example.deskstandard.core.common.result.AppError
import com.example.deskstandard.core.common.result.AppResult
import com.example.deskstandard.core.common.result.map
import com.example.deskstandard.core.database.sample.SampleDao
import com.example.deskstandard.data.sample.api.SampleRepository
import com.example.deskstandard.data.sample.api.model.SampleItem
import com.example.deskstandard.data.sample.local.toDomain
import com.example.deskstandard.data.sample.local.toEntity
import com.example.deskstandard.data.sample.remote.SampleApi
import com.example.deskstandard.data.sample.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

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
internal class OfflineFirstSampleRepository @Inject constructor(
    private val api: SampleApi,
    private val dao: SampleDao,
) : SampleRepository {

    private val readLock = Any()
    private var readers = 0

    // What the readers of [items] saw last, for [peek]. It goes with the last reader, so rows the
    // database has dropped since (a sign-out empties it) are never answered from memory.
    private var lastRead: List<SampleItem>? = null

    override val items: Flow<List<SampleItem>> = dao.observeAll()
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

    override fun peek(id: Int): SampleItem? = synchronized(readLock) { lastRead }?.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> {
        val saved = dao.byId(id)
        return if (saved != null) AppResult.Success(saved.toDomain()) else api.item(id).map { it.toDomain() }
    }

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
