// <opt:!database>
package com.base.app.data.sample

import com.base.app.core.common.result.AppResult
import com.base.app.core.common.result.map
import com.base.app.data.sample.api.SampleRepository
import com.base.app.data.sample.api.model.SampleItem
import com.base.app.data.sample.remote.SampleApi
import com.base.app.data.sample.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Straight from the network, with the last answer kept in memory for this process. Without a
 * database there is nothing to show offline after a restart; the HTTP cache still saves the bytes
 * of an unchanged list.
 */
internal class NetworkSampleRepository(
    private val api: SampleApi,
) : SampleRepository {

    private val fetched = MutableStateFlow<List<SampleItem>>(emptyList())

    override val items: Flow<List<SampleItem>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> = api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

    override fun peek(id: Int): SampleItem? = fetched.value.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
// </opt:!database>
