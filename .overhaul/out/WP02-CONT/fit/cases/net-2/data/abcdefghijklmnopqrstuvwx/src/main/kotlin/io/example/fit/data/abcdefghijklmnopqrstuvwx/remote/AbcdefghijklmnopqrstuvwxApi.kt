package io.example.fit.data.abcdefghijklmnopqrstuvwx.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwx.remote.dto.AbcdefghijklmnopqrstuvwxDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwx endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwx"
    }
}
