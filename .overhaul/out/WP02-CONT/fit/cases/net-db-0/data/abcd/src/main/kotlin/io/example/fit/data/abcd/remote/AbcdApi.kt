package io.example.fit.data.abcd.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcd.remote.dto.AbcdDto
import javax.inject.Inject

/** The abcd endpoints of the app's backend. */
internal class AbcdApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcd"
    }
}
