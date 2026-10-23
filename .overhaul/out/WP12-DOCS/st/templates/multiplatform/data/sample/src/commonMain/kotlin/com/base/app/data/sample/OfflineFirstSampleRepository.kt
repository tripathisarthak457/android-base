// <opt:database>
package com.base.app.data.sample

import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.common.result.AppError
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.result.map
import com.base.app.core.database.sample.SampleDao
import com.base.app.data.sample.api.SampleRepository
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.data.sample.local.toDomain
import com.base.app.data.sample.local.toEntity
import com.base.app.data.sample.remote.SampleApi
import com.base.app.data.sample.remote.toDomain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The database is the source of truth: screens read [items] from it, and [refresh] writes the
 * server's answer into it. What was fetched last is on screen at once, offline included, and the
 * list follows every write.
 */
internal class OfflineFirstSampleRepository(
    private val api: SampleApi,
    private val dao: SampleDao,
    scope: ApplicationScope,
) : SampleRepository {

    /** Kept while someone reads it and briefly after, so a detail opened from the list can [peek]. */
    private val stored: StateFlow<List<SampleItem>?> = dao.observeAll()
        .map { rows -> rows.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.WhileSubscribed(KEEP_AFTER_LAST_READER_MILLIS), null)

    override val items: Flow<List<SampleItem>> = stored.filterNotNull()

    override suspend fun refresh(): AppResult<Unit> = when (val fetched = api.items()) {
        is AppResult.Success -> stored {
            dao.replaceAll(fetched.value.mapIndexed { position, dto -> dto.toEntity(position) })
        }

        is AppResult.Failure -> fetched
    }

    override fun peek(id: Int): SampleItem? = stored.value?.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> {
        val saved = dao.byId(id)
        return if (saved != null) AppResult.Success(saved.toDomain()) else api.item(id).map { it.toDomain() }
    }

    /** A full disk or a damaged file is a failure to report, not a crash at whoever asked. */
    private suspend fun stored(write: suspend () -> Unit): AppResult<Unit> = try {
        write()
        AppResult.Success(Unit)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Exception) {
        AppResult.Failure(AppError.Storage(failure))
    }

    private companion object {
        const val KEEP_AFTER_LAST_READER_MILLIS = 5_000L
    }
}
// </opt:database>
