package com.base.app.data.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.base.app.core.common.result.AppResult
import com.base.app.core.common.result.map
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.get
import com.base.app.data.search.api.SearchRepository
import com.base.app.data.search.api.model.SearchResult
import com.base.app.data.search.di.RecentSearchesStore
import com.base.app.data.search.recent.RecentSearches
import com.base.app.data.search.remote.dto.SearchHitDto
import com.base.app.data.search.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Searches the public demo API's full-text `q` parameter. Point [search] at your own endpoint; the
 * recent searches work unchanged, kept in a file of their own that sign-out empties.
 */
@Singleton
internal class DefaultSearchRepository @Inject constructor(
    private val networkClient: NetworkClient,
    @RecentSearchesStore private val store: DataStore<Preferences>,
) : SearchRepository {

    override val recent: Flow<List<String>> = store.data.map { RecentSearches.decode(it[RECENT]) }

    override suspend fun search(query: String): AppResult<List<SearchResult>> =
        networkClient.get<List<SearchHitDto>>(
            path = POSTS_PATH,
            query = mapOf("q" to query.trim()),
            requiresAuth = false,
        ).map { hits -> hits.map { it.toDomain() } }

    override suspend fun result(id: Int): AppResult<SearchResult> =
        networkClient.get<SearchHitDto>(path = "$POSTS_PATH/$id", requiresAuth = false)
            .map { it.toDomain(snippetLength = Int.MAX_VALUE) }

    override suspend fun remember(query: String) = update { RecentSearches.add(it, query) }

    override suspend fun forget(query: String) = update { RecentSearches.remove(it, query) }

    override suspend fun clearRecent() {
        store.edit { it.remove(RECENT) }
    }

    private suspend fun update(change: (List<String>) -> List<String>) {
        store.edit { it[RECENT] = RecentSearches.encode(change(RecentSearches.decode(it[RECENT]))) }
    }

    private companion object {
        val RECENT = stringPreferencesKey("recent")

        // A public demo API, so the reference search works on first run; a full URL never gets the token.
        const val POSTS_PATH = "https://jsonplaceholder.typicode.com/posts"
    }
}
