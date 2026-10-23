package io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.dto.AbcdefghijklmnopqrstuvwxyDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxy endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxy"
    }
}
