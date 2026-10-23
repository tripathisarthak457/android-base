package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote

import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.network.client.NetworkClient
import io.example.fitcnetdb.core.network.client.get
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote.dto.AbcdefghijklmnopqrstuvwDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvw endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvw"
    }
}
