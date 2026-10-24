package com.example.everything.data.sample.api

import com.example.everything.core.common.result.AppResult
import com.example.everything.data.sample.api.model.SampleItem
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app can ask about samples. An interface, so a ViewModel test injects a fake and
 * never touches a socket.
 */
interface SampleRepository {

    /** The list as last fetched, in the server's order, updated by every [refresh]. Empty before the first. */
    val items: Flow<List<SampleItem>>

    /** Fetches the list again; [items] follows. */
    suspend fun refresh(): AppResult<Unit>

    /**
     * The item from what is already in memory, without any I/O, so a screen opened from the list
     * has its content in its first frame. Null when the list is not in memory.
     */
    fun peek(id: Int): SampleItem?

    /** One item: from the fetched list when it is there, otherwise from the network. */
    suspend fun item(id: Int): AppResult<SampleItem>
}
