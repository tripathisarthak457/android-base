package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.data.__NAME__.api.__P__Repository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The project has no network or database feature, so nothing feeds this domain yet: the list is
 * empty and the screen shows its empty state. Give it a source here; nothing above changes.
 */
@Singleton
internal class Default__P__Repository @Inject constructor() : __P__Repository {

    private val items = emptyList<__P__Item>()

    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> = AppResult.Success(items)

    override suspend fun item(id: String): AppResult<__P__Item> =
        items.firstOrNull { it.id == id }?.let { AppResult.Success(it) }
            ?: AppResult.Failure(AppError.Unexpected(NoSuchElementException("No __NAME__ item $id")))
}
