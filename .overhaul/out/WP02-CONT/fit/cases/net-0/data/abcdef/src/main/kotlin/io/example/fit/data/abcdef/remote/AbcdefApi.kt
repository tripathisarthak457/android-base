package io.example.fit.data.abcdef.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdef.remote.dto.AbcdefDto
import javax.inject.Inject

/** The abcdef endpoints of the app's backend. */
internal class AbcdefApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdef"
    }
}
