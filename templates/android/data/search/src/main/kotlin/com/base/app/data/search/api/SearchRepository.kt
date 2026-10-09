package com.base.app.data.search.api

import com.base.app.core.common.result.AppResult
import kotlinx.coroutines.flow.Flow

interface SearchRepository {

    /** Most recent first, and short: the oldest drop off. Belongs to the signed-in user. */
    val recent: Flow<List<String>>

    suspend fun search(query: String): AppResult<List<SearchResult>>

    suspend fun result(id: Int): AppResult<SearchResult>

    suspend fun remember(query: String)

    suspend fun forget(query: String)

    suspend fun clearRecent()
}
