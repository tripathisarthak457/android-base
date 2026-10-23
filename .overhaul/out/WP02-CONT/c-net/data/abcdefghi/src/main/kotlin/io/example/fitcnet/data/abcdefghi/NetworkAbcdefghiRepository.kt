package io.example.fitcnet.data.abcdefghi

import io.example.fitcnet.core.common.result.AppResult
import io.example.fitcnet.core.common.result.map
import io.example.fitcnet.data.abcdefghi.api.AbcdefghiRepository
import io.example.fitcnet.data.abcdefghi.api.model.AbcdefghiItem
import io.example.fitcnet.data.abcdefghi.remote.AbcdefghiApi
import io.example.fitcnet.data.abcdefghi.remote.toDomain
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
internal class NetworkAbcdefghiRepository @Inject constructor(private val api: AbcdefghiApi) :
    AbcdefghiRepository {

    private val fetched = MutableStateFlow<List<AbcdefghiItem>>(emptyList())

    override val items: Flow<List<AbcdefghiItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: String): AbcdefghiItem? = fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghiItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
