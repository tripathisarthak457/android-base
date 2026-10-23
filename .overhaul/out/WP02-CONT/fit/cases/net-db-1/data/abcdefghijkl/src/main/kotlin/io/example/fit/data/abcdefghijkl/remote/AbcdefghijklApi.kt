package io.example.fit.data.abcdefghijkl.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijkl.remote.dto.AbcdefghijklDto
import javax.inject.Inject

/** The abcdefghijkl endpoints of the app's backend. */
internal class AbcdefghijklApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijkl"
    }
}
