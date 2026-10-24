package __PKG__.data.__NAME__.api

import __PKG__.core.common.result.AppResult
import __PKG__.data.__NAME__.api.model.__P__Item
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app can ask about __TITLE_LOWER__.
 * An interface, so ViewModel tests use a fake.
 */
interface __P__Repository {

    /**
     * The list as last loaded, in its source's order. Every [refresh] and every other write
     * reaches it; it is empty before the first.
     */
    val items: Flow<List<__P__Item>>

    /** Loads the list again; [items] follows. */
    suspend fun refresh(): AppResult<Unit>

    /**
     * The entry from what is already in memory, without any I/O, so a screen opened from the list
     * can have its content in its first frame. Null when the list is not in memory.
     */
    fun peek(id: String): __P__Item?

    /** One entry: from what is already loaded when it is there, otherwise from its source. */
    suspend fun item(id: String): AppResult<__P__Item>
}
