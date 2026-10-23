package io.example.fitcnetdb.data.abcdefghijklmno.remote

import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.network.client.NetworkClient
import io.example.fitcnetdb.core.network.client.get
import io.example.fitcnetdb.data.abcdefghijklmno.remote.dto.AbcdefghijklmnoDto
import javax.inject.Inject

/** The abcdefghijklmno endpoints of the app's backend. */
internal class AbcdefghijklmnoApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghijklmnoDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnoDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmno"
    }
}
