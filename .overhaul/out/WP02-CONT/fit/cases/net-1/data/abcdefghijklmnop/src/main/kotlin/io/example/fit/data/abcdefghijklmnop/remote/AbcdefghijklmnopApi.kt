package io.example.fit.data.abcdefghijklmnop.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnop.remote.dto.AbcdefghijklmnopDto
import javax.inject.Inject

/** The abcdefghijklmnop endpoints of the app's backend. */
internal class AbcdefghijklmnopApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnop"
    }
}
