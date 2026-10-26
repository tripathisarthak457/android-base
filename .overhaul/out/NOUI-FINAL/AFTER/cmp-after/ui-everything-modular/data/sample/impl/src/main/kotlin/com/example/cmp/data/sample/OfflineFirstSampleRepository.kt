package com.example.cmp.data.sample

import com.example.cmp.core.common.result.AppError
import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.common.result.map
import com.example.cmp.core.database.sample.SampleDao
import com.example.cmp.data.sample.api.SampleRepository
import com.example.cmp.data.sample.api.model.SampleItem
import com.example.cmp.data.sample.local.toDomain
import com.example.cmp.data.sample.local.toEntity
import com.example.cmp.data.sample.remote.SampleApi
import com.example.cmp.data.sample.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update

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

    // Who reads [items] and what they saw last, for [peek], changed as one value so readers on any
    // thread keep the count and the list in step without a lock. The list goes with the last reader,
    // so rows the database has dropped since (a sign-out empties it) are never answered from memory.
    private val reads = MutableStateFlow(Reads())

    override val items: Flow<List<SampleItem>> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .onStart { reads.update { it.copy(readers = it.readers + 1) } }
        .onEach { read -> reads.update { it.copy(lastRead = read) } }
        .onCompletion {
            reads.update {
                if (it.readers ==
                    1
                ) {
                    Reads()
                } else {
                    it.copy(readers = it.readers - 1)
                }
            }
        }

    override suspend fun refresh(): AppResult<Unit> = when (val fetched = api.items()) {
        is AppResult.Success -> storing {
            dao.replaceAll(fetched.value.mapIndexed { position, dto -> dto.toEntity(position) })
        }

        is AppResult.Failure -> fetched
    }

    override fun peek(id: Int): SampleItem? = reads.value.lastRead?.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> {
        val saved = dao.byId(id)
        return if (saved !=
            null
        ) {
            AppResult.Success(saved.toDomain())
        } else {
            api.item(id).map { it.toDomain() }
        }
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

    private data class Reads(val readers: Int = 0, val lastRead: List<SampleItem>? = null)
}
