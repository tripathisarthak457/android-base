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
 * Everything the app can ask about samples.
 *
 * An interface, so a ViewModel test injects a fake and never touches a socket. That is the whole
 * of the argument — an interface with one implementation is otherwise exactly the speculative
 * abstraction worth avoiding, and it earns its place here only because the alternative is
 * untestable ViewModels.
 */
interface SampleRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<SampleItem>>

    suspend fun item(id: Int): AppResult<SampleItem>
}

@Singleton
class DefaultSampleRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : SampleRepository {

    /**
     * The list, cached for five minutes.
     *
     * [forceRefresh] is what pull-to-refresh passes. Without it the gesture would hit the cache
     * and appear to do nothing, which is worse than not offering the gesture at all.
     */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<SampleItem>> =
        networkClient.get<List<SampleDto>>(
            path = LIST_PATH,
            cache = if (forceRefresh) {
                CachePolicy.Disabled
            } else {
                CachePolicy.Enabled(key = LIST_CACHE_KEY, maxAgeMillis = CACHE_MAX_AGE_MILLIS)
            },
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
        const val LIST_PATH = "posts"
        const val LIST_CACHE_KEY = "sample:list"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
