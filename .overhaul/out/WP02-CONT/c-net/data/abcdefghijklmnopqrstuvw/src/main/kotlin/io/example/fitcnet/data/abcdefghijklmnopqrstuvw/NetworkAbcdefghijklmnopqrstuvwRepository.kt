package io.example.fitcnet.data.abcdefghijklmnopqrstuvw

import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.common.result.map
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.api.AbcdefghijklmnopqrstuvwRepository
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.remote.AbcdefghijklmnopqrstuvwApi
import io.example.fitcnet.data.abcdefghijklmnopqrstuvw.remote.toDomain
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
internal class NetworkAbcdefghijklmnopqrstuvwRepository @Inject constructor(
    private val api: AbcdefghijklmnopqrstuvwApi,
) : AbcdefghijklmnopqrstuvwRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqrstuvwItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuvwItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqrstuvwItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
