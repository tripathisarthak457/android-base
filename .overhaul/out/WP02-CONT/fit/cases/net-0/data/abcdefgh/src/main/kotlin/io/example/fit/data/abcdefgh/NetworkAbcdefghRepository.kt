package io.example.fit.data.abcdefgh

import io.example.fit.core.common.result.AppResult
import io.example.fit.core.common.result.map
import io.example.fit.data.abcdefgh.api.AbcdefghRepository
import io.example.fit.data.abcdefgh.api.model.AbcdefghItem
import io.example.fit.data.abcdefgh.remote.AbcdefghApi
import io.example.fit.data.abcdefgh.remote.toDomain
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
internal class NetworkAbcdefghRepository @Inject constructor(private val api: AbcdefghApi) :
    AbcdefghRepository {

    private val fetched = MutableStateFlow<List<AbcdefghItem>>(emptyList())

    override val items: Flow<List<AbcdefghItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghItem? = fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
