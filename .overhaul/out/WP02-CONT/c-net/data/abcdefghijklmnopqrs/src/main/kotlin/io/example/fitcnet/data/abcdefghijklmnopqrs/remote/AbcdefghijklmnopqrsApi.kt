package io.example.fitcnet.data.abcdefghijklmnopqrs.remote

import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.network.client.NetworkClient
import io.example.fitcnet.core.network.client.get
import io.example.fitcnet.data.abcdefghijklmnopqrs.remote.dto.AbcdefghijklmnopqrsDto
import javax.inject.Inject

/** The abcdefghijklmnopqrs endpoints of the app's backend. */
internal class AbcdefghijklmnopqrsApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrsDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrsDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrs"
    }
}
