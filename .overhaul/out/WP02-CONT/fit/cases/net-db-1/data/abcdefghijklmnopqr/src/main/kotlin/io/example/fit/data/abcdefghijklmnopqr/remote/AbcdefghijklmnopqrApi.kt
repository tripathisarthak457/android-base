package io.example.fit.data.abcdefghijklmnopqr.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqr.remote.dto.AbcdefghijklmnopqrDto
import javax.inject.Inject

/** The abcdefghijklmnopqr endpoints of the app's backend. */
internal class AbcdefghijklmnopqrApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqr"
    }
}
