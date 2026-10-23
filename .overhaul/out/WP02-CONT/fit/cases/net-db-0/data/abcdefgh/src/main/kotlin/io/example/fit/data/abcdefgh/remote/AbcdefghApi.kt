package io.example.fit.data.abcdefgh.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefgh.remote.dto.AbcdefghDto
import javax.inject.Inject

/** The abcdefgh endpoints of the app's backend. */
internal class AbcdefghApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefgh"
    }
}
