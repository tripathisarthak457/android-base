package io.example.fit.data.abcdefghijklmnopqrst.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrst.remote.dto.AbcdefghijklmnopqrstDto
import javax.inject.Inject

/** The abcdefghijklmnopqrst endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrst"
    }
}
