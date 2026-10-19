package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppResult
import __PKG__.core.common.result.map
import __PKG__.core.network.client.NetworkClient
import __PKG__.core.network.client.get
import __PKG__.data.__NAME__.api.__P__Item
import __PKG__.data.__NAME__.api.__P__Repository
import __PKG__.data.__NAME__.remote.__P__Dto
import __PKG__.data.__NAME__.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
__FIT__
internal class Default__P__Repository @Inject constructor(private val networkClient: NetworkClient) :
    __P__Repository {
__ELSE__
internal class Default__P__Repository @Inject constructor(
    private val networkClient: NetworkClient,
) : __P__Repository {
__END_FIT__

    /**
     * The HTTP cache keeps each answer for as long as the server's headers allow, so reopening the
     * screen or going offline shows the last one. A refresh asks the server again instead.
     */
__FIT__
    override suspend fun items(forceRefresh: Boolean): AppResult<List<__P__Item>> =
        networkClient.get<List<__P__Dto>>(
            path = PATH,
            headers = if (forceRefresh) REVALIDATE else emptyMap(),
        ).map { dtos -> dtos.map(__P__Dto::toDomain) }
__ELSE__
    override suspend fun items(
        forceRefresh: Boolean,
    ): AppResult<List<__P__Item>> =
        networkClient.get<List<__P__Dto>>(
            path = PATH,
            headers = if (forceRefresh) REVALIDATE else emptyMap(),
        ).map { dtos -> dtos.map(__P__Dto::toDomain) }
__END_FIT__

__FIT__
    override suspend fun item(id: String): AppResult<__P__Item> =
        networkClient.get<__P__Dto>(path = "$PATH/$id").map(__P__Dto::toDomain)
__ELSE__
    override suspend fun item(id: String): AppResult<__P__Item> =
        networkClient.get<__P__Dto>(
            path = "$PATH/$id",
        ).map(__P__Dto::toDomain)
__END_FIT__

    private companion object {
        const val PATH = "__NAME__"

        /** Makes the HTTP cache check with the server rather than answer from what it holds. */
        val REVALIDATE = mapOf("Cache-Control" to "no-cache")
    }
}
