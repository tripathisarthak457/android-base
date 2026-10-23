package io.example.fit.data.abcdefghijklmnopq

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefghijklmnopq.api.AbcdefghijklmnopqRepository
import io.example.fit.data.abcdefghijklmnopq.api.model.AbcdefghijklmnopqItem
import io.example.fit.data.abcdefghijklmnopq.remote.AbcdefghijklmnopqApi
import io.example.fit.data.abcdefghijklmnopq.remote.toDomain
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
internal class NetworkAbcdefghijklmnopqRepository @Inject constructor(
    private val api: AbcdefghijklmnopqApi,
) : AbcdefghijklmnopqRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
