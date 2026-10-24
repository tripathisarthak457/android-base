package com.example.deskstandard.data.sample

import com.example.deskstandard.core.common.result.AppError
import com.example.deskstandard.core.common.result.AppResult
import com.example.deskstandard.core.network.client.NetworkClient
import com.example.deskstandard.core.network.client.NetworkJson
import com.example.deskstandard.core.network.model.NetworkRequest
import com.example.deskstandard.core.network.model.NetworkResponse
import com.example.deskstandard.data.sample.remote.dto.SampleDto
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.builtins.ListSerializer

/** Answers the sample endpoints from a list, or fails every call with [failure]. */
internal class FakeSampleClient(var posts: List<SampleDto>) : NetworkClient {

    var failure: AppError? = null
    val paths = mutableListOf<String>()

    override suspend fun execute(request: NetworkRequest): AppResult<NetworkResponse> {
        paths += request.path
        failure?.let { return AppResult.Failure(it) }
        val id = request.path.substringAfterLast('/').toIntOrNull()
        val body = if (id == null) {
            NetworkJson.encodeToString(ListSerializer(SampleDto.serializer()), posts)
        } else {
            NetworkJson.encodeToString(SampleDto.serializer(), posts.first { it.id == id })
        }
        return AppResult.Success(NetworkResponse(200, body))
    }

    override suspend fun <T> execute(request: NetworkRequest, deserializer: DeserializationStrategy<T>): AppResult<T> =
        when (val result = execute(request)) {
            is AppResult.Success -> AppResult.Success(NetworkJson.decodeFromString(deserializer, result.value.body))
            is AppResult.Failure -> result
        }
}
