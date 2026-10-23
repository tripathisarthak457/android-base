package io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd

import io.example.fitclocal.core.common.result.AppError
import io.example.fitclocal.core.common.result.AppResult
import io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.api.AbcdefghijklmnopqrstuvwxyzabcdRepository
import io.example.fitclocal.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
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
internal class DefaultAbcdefghijklmnopqrstuvwxyzabcdRepository @Inject constructor() :
    AbcdefghijklmnopqrstuvwxyzabcdRepository {

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxyzabcdItem>> = flowOf(emptyList())

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxyzabcdItem? = null

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabcdItem> =
        AppResult.Failure(AppError.Unexpected(NoSuchElementException(id)))
}
