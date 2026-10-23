package io.example.fit.data.abcdefghijklmnopq.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopq.remote.dto.AbcdefghijklmnopqDto
import javax.inject.Inject

/** The abcdefghijklmnopq endpoints of the app's backend. */
internal class AbcdefghijklmnopqApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopq"
    }
}
