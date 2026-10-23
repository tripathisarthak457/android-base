package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.dto.AbcdefghijklmnopqrstuvwxyzabDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxyzab endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyzabApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyzabDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabDto> =
        client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxyzab"
    }
}
