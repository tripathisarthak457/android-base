package com.base.app.data.sample.remote

import com.base.app.core.common.result.AppResult
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.get
import com.base.app.data.sample.remote.dto.SampleDto

/** The sample endpoints. */
internal class SampleApi(
    private val client: NetworkClient,
) {
    suspend fun items(): AppResult<List<SampleDto>> = client.get(LIST_PATH)

    suspend fun item(id: Int): AppResult<SampleDto> = client.get("$LIST_PATH/$id")

    private companion object {
        // A public demo API rather than the app's own backend, so the reference feature works on
        // first run whatever the backend URL is set to.
        const val LIST_PATH = "https://jsonplaceholder.typicode.com/posts"
    }
}
