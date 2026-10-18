package com.example.wp04.everything.data.sample.api

import com.example.wp04.everything.core.common.result.AppResult

/**
 * Everything the app can ask about samples. An interface, so a ViewModel test injects a fake and
 * never touches a socket.
 */
interface SampleRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<SampleItem>>

    suspend fun item(id: Int): AppResult<SampleItem>
}
