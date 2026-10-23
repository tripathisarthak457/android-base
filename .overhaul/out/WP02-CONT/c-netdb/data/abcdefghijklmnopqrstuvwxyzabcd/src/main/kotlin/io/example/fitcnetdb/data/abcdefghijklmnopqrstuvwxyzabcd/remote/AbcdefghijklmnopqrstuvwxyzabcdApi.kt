package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote

import io.example.fitcnetdb.core.common.result.AppResult
import io.example.fitcnetdb.core.network.client.NetworkClient
import io.example.fitcnetdb.core.network.client.get
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxyzabcd endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyzabcdApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyzabcdDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabcdDto> =
        client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxyzabcd"
    }
}
