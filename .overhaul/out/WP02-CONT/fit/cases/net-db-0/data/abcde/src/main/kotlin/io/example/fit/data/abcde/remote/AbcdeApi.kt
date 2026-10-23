package io.example.fit.data.abcde.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcde.remote.dto.AbcdeDto
import javax.inject.Inject

/** The abcde endpoints of the app's backend. */
internal class AbcdeApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdeDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdeDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcde"
    }
}
