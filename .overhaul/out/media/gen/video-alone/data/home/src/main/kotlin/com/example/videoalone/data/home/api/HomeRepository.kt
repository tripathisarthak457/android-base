package com.example.videoalone.data.home.api

import com.example.videoalone.core.common.result.AppResult

/**
 * Everything the app can ask about home.
 * An interface, so ViewModel tests use a fake.
 */
interface HomeRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<HomeItem>>

    suspend fun item(id: String): AppResult<HomeItem>
}
