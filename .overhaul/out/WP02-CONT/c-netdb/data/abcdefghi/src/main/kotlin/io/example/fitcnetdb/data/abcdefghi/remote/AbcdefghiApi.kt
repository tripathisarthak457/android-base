package io.example.fitcnetdb.data.abcdefghi.remote

import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.network.client.NetworkClient
import io.example.fitcnetdb.core.network.client.get
import io.example.fitcnetdb.data.abcdefghi.remote.dto.AbcdefghiDto
import javax.inject.Inject

/** The abcdefghi endpoints of the app's backend. */
internal class AbcdefghiApi @Inject constructor(private val client: NetworkClient) {
    suspend fun items(): AppResult<List<AbcdefghiDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghiDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghi"
    }
}
