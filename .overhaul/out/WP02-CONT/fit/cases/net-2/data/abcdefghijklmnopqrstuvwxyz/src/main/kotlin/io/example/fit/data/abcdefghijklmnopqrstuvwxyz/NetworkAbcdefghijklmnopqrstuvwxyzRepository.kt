package io.example.fit.data.abcdefghijklmnopqrstuvwxyz

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.AbcdefghijklmnopqrstuvwxyzRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.model.AbcdefghijklmnopqrstuvwxyzItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.AbcdefghijklmnopqrstuvwxyzApi
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Straight from the network, with the last answer kept in memory for this process. Without a
 * database there is nothing to show offline after a restart; the HTTP cache still saves the bytes
 * of an unchanged list.
 */
@Singleton
internal class NetworkAbcdefghijklmnopqrstuvwxyzRepository @Inject constructor(
    private val api: AbcdefghijklmnopqrstuvwxyzApi,
) : AbcdefghijklmnopqrstuvwxyzRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqrstuvwxyzItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxyzItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxyzItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
