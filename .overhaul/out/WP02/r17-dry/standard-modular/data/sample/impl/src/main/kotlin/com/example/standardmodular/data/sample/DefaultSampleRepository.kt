package com.example.standardmodular.data.sample

import com.example.standardmodular.core.common.result.AppResult
import com.example.standardmodular.core.common.result.map
import com.example.standardmodular.core.network.NetworkClient
import com.example.standardmodular.core.network.get
import com.example.standardmodular.core.network.model.CachePolicy
import com.example.standardmodular.data.sample.api.SampleItem
import com.example.standardmodular.data.sample.api.SampleRepository
import com.example.standardmodular.data.sample.remote.SampleDto
import com.example.standardmodular.data.sample.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultSampleRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : SampleRepository {

    /** The list, cached for five minutes. [forceRefresh] is what pull-to-refresh passes. */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<SampleItem>> =
        networkClient.get<List<SampleDto>>(
            path = LIST_PATH,
            // Refreshing still saves the answer, and still falls back to the saved list offline.
            cache = CachePolicy.Enabled(
                key = LIST_CACHE_KEY,
                maxAgeMillis = CACHE_MAX_AGE_MILLIS,
                forceRefresh = forceRefresh,
            ),
            requiresAuth = false,
        ).map { it.toDomain() }

    override suspend fun item(id: Int): AppResult<SampleItem> =
        networkClient.get<SampleDto>(
            path = "$LIST_PATH/$id",
            cache = CachePolicy.Enabled(
                key = "$LIST_CACHE_KEY:$id",
                maxAgeMillis = CACHE_MAX_AGE_MILLIS,
            ),
            requiresAuth = false,
        ).map { it.toDomain() }

    private companion object {
        // A public demo API rather than the app's own backend, so the reference feature works on
        // first run whatever the backend URLs are set to. Absolute URLs never receive the user's token.
        const val LIST_PATH = "https://jsonplaceholder.typicode.com/posts"
        const val LIST_CACHE_KEY = "sample:list"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
