package com.base.app.data.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.common.AppResult
import com.base.app.core.common.map
import com.base.app.core.datastore.di.SessionDataStore
import com.base.app.core.network.NetworkClient
import com.base.app.core.network.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val id: Int,
    val title: String,
    val snippet: String,
)

interface SearchRepository {

    /** Most recent first, at most [RecentSearches.LIMIT]. Belongs to the signed-in user. */
    val recent: Flow<List<String>>

    suspend fun search(query: String): AppResult<List<SearchResult>>

    suspend fun result(id: Int): AppResult<SearchResult>

    suspend fun remember(query: String)

    suspend fun forget(query: String)

    suspend fun clearRecent()
}

/**
 * Searches the public demo API's full-text `q` parameter. Point [search] at your own endpoint; the
 * recent-searches half works unchanged.
 */
@Singleton
class DefaultSearchRepository @Inject constructor(
    private val networkClient: NetworkClient,
    @SessionDataStore private val store: DataStore<Preferences>,
) : SearchRepository {

    override val recent: Flow<List<String>> = store.data.map { RecentSearches.decode(it[RECENT]) }

    override suspend fun search(query: String): AppResult<List<SearchResult>> =
        networkClient.get<List<SearchHitDto>>(
            path = "posts",
            query = mapOf("q" to query.trim()),
            requiresAuth = false,
        ).map { hits -> hits.map { it.toDomain() } }

    override suspend fun result(id: Int): AppResult<SearchResult> =
        networkClient.get<SearchHitDto>(path = "posts/$id", requiresAuth = false)
            .map { it.toDomain(snippetLength = Int.MAX_VALUE) }

    override suspend fun remember(query: String) {
        store.edit { it[RECENT] = RecentSearches.encode(RecentSearches.add(RecentSearches.decode(it[RECENT]), query)) }
    }

    override suspend fun forget(query: String) {
        store.edit { it[RECENT] = RecentSearches.encode(RecentSearches.decode(it[RECENT]) - query) }
    }

    override suspend fun clearRecent() {
        store.edit { it.remove(RECENT) }
    }

    private companion object {
        val RECENT = stringPreferencesKey("search.recent")
    }
}

/** The list logic, apart from storage, so it can be tested without a DataStore. */
internal object RecentSearches {
    const val LIMIT = 8
    private const val SEPARATOR = '\u001F'

    fun add(current: List<String>, query: String): List<String> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return current
        return (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }).take(LIMIT)
    }

    // A unit separator rather than JSON: the values are single lines a person typed, and this
    // cannot fail to parse.
    fun encode(values: List<String>): String = values.joinToString(SEPARATOR.toString())

    fun decode(raw: String?): List<String> = raw?.split(SEPARATOR)?.filter { it.isNotBlank() }.orEmpty()
}

@Serializable
internal data class SearchHitDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
)

internal fun SearchHitDto.toDomain(snippetLength: Int = SNIPPET_LENGTH) = SearchResult(
    id = id,
    title = title.trim().replaceFirstChar { it.uppercase() },
    snippet = body.replace('\n', ' ').trim().take(snippetLength),
)

private const val SNIPPET_LENGTH = 120
