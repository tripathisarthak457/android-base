package io.example.fit.data.a.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.a.remote.dto.ADto
import javax.inject.Inject

/** The a endpoints of the app's backend. */
internal class AApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<ADto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<ADto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "a"
    }
}
