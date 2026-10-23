package io.example.fit.data.abcdefg.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefg.remote.dto.AbcdefgDto
import javax.inject.Inject

/** The abcdefg endpoints of the app's backend. */
internal class AbcdefgApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefgDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefgDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefg"
    }
}
