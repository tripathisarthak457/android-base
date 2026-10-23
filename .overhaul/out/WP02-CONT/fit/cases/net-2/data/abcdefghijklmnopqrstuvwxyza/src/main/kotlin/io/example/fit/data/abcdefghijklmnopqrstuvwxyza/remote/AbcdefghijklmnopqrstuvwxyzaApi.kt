package io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote.dto.AbcdefghijklmnopqrstuvwxyzaDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxyza endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyzaApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyzaDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzaDto> =
        client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxyza"
    }
}
