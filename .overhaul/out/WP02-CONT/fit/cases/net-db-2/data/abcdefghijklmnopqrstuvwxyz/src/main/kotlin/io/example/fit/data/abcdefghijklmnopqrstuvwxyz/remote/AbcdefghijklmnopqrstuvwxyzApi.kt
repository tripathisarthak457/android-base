package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.network.client.NetworkClient
import io.example.fit.core.network.client.get
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.dto.AbcdefghijklmnopqrstuvwxyzDto
import javax.inject.Inject

/** The abcdefghijklmnopqrstuvwxyz endpoints of the app's backend. */
internal class AbcdefghijklmnopqrstuvwxyzApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<AbcdefghijklmnopqrstuvwxyzDto>> = client.get(PATH)

    suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzDto> = client.get("$PATH/$id")

    private companion object {
        /** Relative to the backend's base URL, so every call carries the session's token. */
        const val PATH = "abcdefghijklmnopqrstuvwxyz"
    }
}
