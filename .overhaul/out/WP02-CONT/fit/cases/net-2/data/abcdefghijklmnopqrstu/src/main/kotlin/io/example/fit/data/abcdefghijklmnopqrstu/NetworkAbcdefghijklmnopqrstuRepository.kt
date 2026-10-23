package io.example.fit.data.abcdefghijklmnopqrstu

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefghijklmnopqrstu.api.AbcdefghijklmnopqrstuRepository
import io.example.fit.data.abcdefghijklmnopqrstu.api.model.AbcdefghijklmnopqrstuItem
import io.example.fit.data.abcdefghijklmnopqrstu.remote.AbcdefghijklmnopqrstuApi
import io.example.fit.data.abcdefghijklmnopqrstu.remote.toDomain
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
internal class NetworkAbcdefghijklmnopqrstuRepository @Inject constructor(
    private val api: AbcdefghijklmnopqrstuApi,
) : AbcdefghijklmnopqrstuRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqrstuItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqrstuItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
