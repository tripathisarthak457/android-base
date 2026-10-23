package io.example.fit.data.abcdefghijklm.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklm.remote.dto.AbcdefghijklmDto
import javax.inject.Inject

/** The abcdefghijklm endpoints of the app's backend. */
internal class AbcdefghijklmApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklm"
    }
}
