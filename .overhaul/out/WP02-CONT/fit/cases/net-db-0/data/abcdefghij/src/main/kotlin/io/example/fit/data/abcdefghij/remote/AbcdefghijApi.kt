package io.example.fit.data.abcdefghij.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghij.remote.dto.AbcdefghijDto
import javax.inject.Inject

/** The abcdefghij endpoints of the app's backend. */
internal class AbcdefghijApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghij"
    }
}
