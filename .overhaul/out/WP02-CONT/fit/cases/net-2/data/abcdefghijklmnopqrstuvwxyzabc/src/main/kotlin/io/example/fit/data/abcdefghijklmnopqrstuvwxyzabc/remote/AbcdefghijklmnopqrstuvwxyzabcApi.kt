package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote.dto.AbcdefghijklmnopqrstuvwxyzabcDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxyzabc endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyzabcApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyzabcDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabcDto> =
        client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxyzabc"
    }
}
