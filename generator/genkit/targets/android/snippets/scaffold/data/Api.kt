package __PKG__.data.__NAME__.remote

import __PKG__.core.common.result.AppResult
import __PKG__.core.network.client.NetworkClient
import __PKG__.core.network.client.get
import __PKG__.data.__NAME__.remote.dto.__P__Dto
import javax.inject.Inject

/** The __TITLE_LOWER__ endpoints of the app's backend. */
__FIT__
internal class __P__Api @Inject constructor(private val client: NetworkClient) {
__ELSE__
internal class __P__Api @Inject constructor(
    private val client: NetworkClient,
) {
__END_FIT__
    suspend fun items(): AppResult<List<__P__Dto>> = client.get(PATH)

__FIT__
    suspend fun item(id: String): AppResult<__P__Dto> = client.get("$PATH/$id")
__ELSE__
    suspend fun item(id: String): AppResult<__P__Dto> =
        client.get("$PATH/$id")
__END_FIT__

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "__NAME__"
    }
}
