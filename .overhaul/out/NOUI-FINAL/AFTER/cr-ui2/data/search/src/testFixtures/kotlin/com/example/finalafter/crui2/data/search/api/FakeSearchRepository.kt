package com.example.finalafter.crui2.data.search.api

import com.example.finalafter.crui2.core.common.result.AppResult
import com.example.finalafter.crui2.data.search.api.model.SearchResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A [SearchRepository] that answers every query with [results] and records each query it was
 * asked, in [searched]. While [writes] is set, every change to the recent searches waits for it, as
 * a DataStore edit waits for the disk.
 */
class FakeSearchRepository(
    private val results: List<SearchResult> = listOf(SearchResult(1, "A title", "A snippet")),
    recent: List<String> = emptyList(),
) : SearchRepository {

    val searched = mutableListOf<String>()
    var writes: CompletableDeferred<Unit>? = null

    private val recentQueries = MutableStateFlow(recent)

    override val recent: StateFlow<List<String>> = recentQueries

    override suspend fun search(query: String): AppResult<List<SearchResult>> {
        searched += query
        return AppResult.Success(results)
    }

    override suspend fun result(id: Int): AppResult<SearchResult> = AppResult.Success(results.first())

    override suspend fun remember(query: String) {
        writes?.await()
        recentQueries.value = listOf(query) + recentQueries.value
    }

    override suspend fun forget(query: String) {
        writes?.await()
        recentQueries.value = recentQueries.value - query
    }

    override suspend fun clearRecent() {
        writes?.await()
        recentQueries.value = emptyList()
    }
}
