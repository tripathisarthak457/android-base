package io.example.fit.data.abcdefghijklmn.api

import io.example.fit.core.common.result.AppResult
import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app can ask about abcdefghijklmn.
 * An interface, so ViewModel tests use a fake.
 */
interface AbcdefghijklmnRepository {

    /**
     * The list as last loaded, in its source's order. Every [refresh] and every other write
     * reaches it; it is empty before the first.
     */
    val items: Flow<List<AbcdefghijklmnItem>>

    /** Loads the list again; [items] follows. */
    suspend fun refresh(): AppResult<Unit>

    /**
     * The entry from what is already in memory, without any I/O, so a screen opened from the list
     * can have its content in its first frame. Null when the list is not in memory.
     */
    fun peek(id: String): AbcdefghijklmnItem?

    /** One entry: from what is already loaded when it is there, otherwise from its source. */
    suspend fun item(id: String): AppResult<AbcdefghijklmnItem>
}
