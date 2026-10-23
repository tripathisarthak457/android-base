package com.base.app.data.feed

import com.base.app.core.common.result.AppResult
import com.base.app.core.network.client.NetworkClient
import com.base.app.core.network.client.NetworkJson
import com.base.app.core.network.model.NetworkRequest
import com.base.app.core.network.model.NetworkResponse
import com.base.app.data.feed.remote.dto.FeedPostDto
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer

/** Answers `_page` and `_limit` the way the demo API does, from [total] numbered posts. */
internal class FakeFeedClient(private val total: Int = 100) : NetworkClient {

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
        val page = request.query.getValue("_page") as Int
        val limit = request.query.getValue("_limit") as Int
        val first = (page - 1) * limit + 1
        val posts = (first..minOf(first + limit - 1, total)).map { id -> FeedPostDto(id = id, title = "Post $id") }
        return AppResult.Success(NetworkResponse(200, NetworkJson.encodeToString(ListSerializer(FeedPostDto.serializer()), posts)))
    }

    override suspend fun <T> execute(request: NetworkRequest, deserializer: DeserializationStrategy<T>): AppResult<T> =
        when (val result = execute(request)) {
            is AppResult.Success -> AppResult.Success(NetworkJson.decodeFromString(deserializer, result.value.body))
            is AppResult.Failure -> result
        }
}
