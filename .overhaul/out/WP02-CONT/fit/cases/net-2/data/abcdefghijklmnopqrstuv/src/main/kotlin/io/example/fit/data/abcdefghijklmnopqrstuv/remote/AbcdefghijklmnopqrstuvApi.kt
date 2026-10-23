package io.example.fit.data.abcdefghijklmnopqrstuv.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuv.remote.dto.AbcdefghijklmnopqrstuvDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuv endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuv"
    }
}
