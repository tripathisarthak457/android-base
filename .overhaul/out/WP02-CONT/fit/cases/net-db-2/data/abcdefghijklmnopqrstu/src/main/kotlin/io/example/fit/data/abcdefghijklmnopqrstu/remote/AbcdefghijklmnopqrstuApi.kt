package io.example.fit.data.abcdefghijklmnopqrstu.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstu.remote.dto.AbcdefghijklmnopqrstuDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstu endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstu"
    }
}
