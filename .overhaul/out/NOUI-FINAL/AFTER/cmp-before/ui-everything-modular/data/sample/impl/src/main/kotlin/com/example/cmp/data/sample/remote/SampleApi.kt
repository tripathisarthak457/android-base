package com.example.cmp.data.sample.remote

import com.example.cmp.core.common.result.AppResult
import com.example.cmp.core.network.client.NetworkClient
import com.example.cmp.core.network.client.get
import com.example.cmp.data.sample.remote.dto.SampleDto
import javax.inject.Inject

/** The sample endpoints. */
internal class SampleApi @Inject constructor(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<SampleDto>> = client.get(LIST_PATH, requiresAuth = false)

    suspend fun item(id: Int): AppResult<SampleDto> = client.get("$LIST_PATH/$id", requiresAuth = false)

    private companion object {
        // A public demo API rather than the app's own backend, so the reference feature works on
        // first run whatever the backend URLs are set to. A full URL never receives the session's token.
        const val LIST_PATH = "https://jsonplaceholder.typicode.com/posts"
    }
}
