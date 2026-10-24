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
__FIT__
internal class Default__P__Repository @Inject constructor() : __P__Repository {
__ELSE__
internal class Default__P__Repository @Inject constructor() :
    __P__Repository {
__END_FIT__

    private val items = emptyList<__P__Item>()

__FIT__
    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> =
        AppResult.Success(items)
__ELSE__
    override suspend fun items(
        forceRefresh: Boolean,
    ): AppResult<List<__P__Item>> = AppResult.Success(items)
__END_FIT__

    override suspend fun item(id: String): AppResult<__P__Item> {
        val item = items.firstOrNull { it.id == id } ?: return AppResult.Failure(
__FIT__
            AppError.Unexpected(NoSuchElementException("No __NAME__ item $id")),
__ELSE__
            AppError.Unexpected(
                NoSuchElementException("No __NAME__ item $id"),
            ),
__END_FIT__
        )
        return AppResult.Success(item)
    }
}
