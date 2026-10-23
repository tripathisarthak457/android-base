package io.example.fit.data.abcdefghijklmnopqrstuvwx

import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.AbcdefghijklmnopqrstuvwxRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.model.AbcdefghijklmnopqrstuvwxItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * The project has no network, so nothing feeds this domain yet: the list is empty and the screen
 * shows its empty state. Give it a source here, an API or a table in `:core:database`; nothing
 * above changes.
 */
@Singleton
internal class DefaultAbcdefghijklmnopqrstuvwxRepository @Inject constructor() :
    AbcdefghijklmnopqrstuvwxRepository {

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxItem>> = flowOf(emptyList())

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxItem? = null

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxItem> =
        AppResult.Failure(AppError.Unexpected(NoSuchElementException(id)))
}
