package com.base.app.data.sample

import com.base.app.core.common.AppResult
import com.base.app.core.common.map
import com.base.app.core.network.NetworkClient
import com.base.app.core.network.get
import com.base.app.core.network.model.CachePolicy
import com.base.app.data.sample.remote.SampleDto
import com.base.app.data.sample.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the app can ask about samples. An interface, so a ViewModel test injects a fake and
 * never touches a socket.
 */
interface SampleRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<SampleItem>>

    suspend fun item(id: Int): AppResult<SampleItem>
}

@Singleton
class DefaultSampleRepository @Inject constructor(
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
