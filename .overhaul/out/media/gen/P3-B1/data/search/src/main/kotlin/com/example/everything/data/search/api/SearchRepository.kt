package com.example.everything.data.search.api

import com.example.everything.core.common.result.AppResult
import com.example.everything.data.search.api.model.SearchResult
import kotlinx.coroutines.flow.Flow

/** Searching, and the signed-in person's recent searches. */
interface SearchRepository {

    /** Most recent first, and short: the oldest drop off. Belongs to the signed-in person. */
    val recent: Flow<List<String>>

    /** The results for [query], in the server's order of relevance. */
    suspend fun search(query: String): AppResult<List<SearchResult>>

    /** One result, for a screen opened from the list. */
    suspend fun result(id: Int): AppResult<SearchResult>

    /** Puts [query] at the top of [recent], once. */
    suspend fun remember(query: String)

    suspend fun forget(query: String)

    suspend fun clearRecent()
}
