package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppResult
import __PKG__.core.common.result.map
import __PKG__.core.network.CachePolicy
import __PKG__.core.network.NetworkClient
import __PKG__.core.network.get
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.data.__NAME__.api.__P__Repository
import __PKG__.data.__NAME__.remote.__P__Dto
import __PKG__.data.__NAME__.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class Default__P__Repository @Inject constructor(
    private val networkClient: NetworkClient,
) : __P__Repository {

    /** Cached, so reopening the screen or going offline shows the last answer. */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> =
        networkClient.get<List<__P__Dto>>(
            path = PATH,
            cache = CachePolicy.Enabled(key = PATH, maxAgeMillis = CACHE_MAX_AGE_MILLIS, forceRefresh = forceRefresh),
        ).map { dtos -> dtos.map(__P__Dto::toDomain) }

    override suspend fun item(id: String): AppResult<__P__Item> =
        networkClient.get<__P__Dto>(
            path = "$PATH/$id",
            cache = CachePolicy.Enabled(key = "$PATH/$id", maxAgeMillis = CACHE_MAX_AGE_MILLIS),
        ).map(__P__Dto::toDomain)

    private companion object {
        const val PATH = "__NAME__"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
