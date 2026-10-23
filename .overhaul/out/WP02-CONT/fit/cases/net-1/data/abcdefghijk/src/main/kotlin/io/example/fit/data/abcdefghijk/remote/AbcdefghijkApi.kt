package io.example.fit.data.abcdefghijk.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijk.remote.dto.AbcdefghijkDto
import javax.inject.Inject

/** The abcdefghijk endpoints of the app's backend. */
internal class AbcdefghijkApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijkDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijkDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijk"
    }
}
