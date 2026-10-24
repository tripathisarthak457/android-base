package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.data.__NAME__.api.__P__Repository
import __PKG__.data.__NAME__.api.model.__P__Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The project has no network, so nothing feeds this domain yet: the list is empty and the screen
 * shows its empty state. Give it a source here, an API or a table in `:core:database`; nothing
 * above changes.
 */
@Singleton
__FIT__
internal class Default__P__Repository @Inject constructor() : __P__Repository {
__ELSE__
internal class Default__P__Repository @Inject constructor() :
    __P__Repository {
__END_FIT__

    override val items: Flow<List<__P__Item>> = flowOf(emptyList())

    override suspend fun refresh(): AppResult<Unit> = AppResult.Success(Unit)

    override fun peek(id: String): __P__Item? = null

    override suspend fun item(id: String): AppResult<__P__Item> =
        AppResult.Failure(AppError.Unexpected(NoSuchElementException(id)))
}
