package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.AbcdefghijklmnopqrstuvwxyzabRepository
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.AbcdefghijklmnopqrstuvwxyzabApi
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.toDomain
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
internal class NetworkAbcdefghijklmnopqrstuvwxyzabRepository @Inject constructor(
    private val api: AbcdefghijklmnopqrstuvwxyzabApi,
) : AbcdefghijklmnopqrstuvwxyzabRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnopqrstuvwxyzabItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxyzabItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxyzabItem? =
        fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
