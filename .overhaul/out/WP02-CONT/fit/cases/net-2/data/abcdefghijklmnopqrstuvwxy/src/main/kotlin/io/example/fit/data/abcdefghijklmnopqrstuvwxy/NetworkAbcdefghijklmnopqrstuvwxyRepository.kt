package io.example.fit.data.abcdefghijklmnopqrstuvwxy

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.AbcdefghijklmnopqrstuvwxyRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.AbcdefghijklmnopqrstuvwxyApi
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.toDomain
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
internal class NetworkAbcdefghijklmnopqrstuvwxyRepository @Inject constructor(
    private val api: AbcdefghijklmnopqrstuvwxyApi,
) : AbcdefghijklmnopqrstuvwxyRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqrstuvwxyItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxyItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxyItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
