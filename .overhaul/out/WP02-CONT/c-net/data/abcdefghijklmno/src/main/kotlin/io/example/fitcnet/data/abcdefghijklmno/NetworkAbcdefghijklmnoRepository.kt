package io.example.fitcnet.data.abcdefghijklmno

import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.common.result.map
import io.example.fitcnet.data.abcdefghijklmno.api.AbcdefghijklmnoRepository
import io.example.fitcnet.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem
import io.example.fitcnet.data.abcdefghijklmno.remote.AbcdefghijklmnoApi
import io.example.fitcnet.data.abcdefghijklmno.remote.toDomain
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
internal class NetworkAbcdefghijklmnoRepository @Inject constructor(
    private val api: AbcdefghijklmnoApi,
) : AbcdefghijklmnoRepository {

    private val fetched = MutableStateFlow<List<AbcdefghijklmnoItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnoItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghijklmnoItem? = fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnoItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
