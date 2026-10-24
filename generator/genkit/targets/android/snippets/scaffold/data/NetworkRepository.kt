package __PKG__.data.__NAME__

import __PKG__.core.common.result.AppResult
import __PKG__.core.common.result.map
import __PKG__.data.__NAME__.api.__P__Repository
import __PKG__.data.__NAME__.api.model.__P__Item
import __PKG__.data.__NAME__.remote.__P__Api
import __PKG__.data.__NAME__.remote.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Straight from the network, with the last answer kept in memory for this process. Without a
 * database there is nothing to show offline after a restart; the HTTP cache still saves the bytes
 * of an unchanged list.
 */
@Singleton
__FIT__
internal class Network__P__Repository @Inject constructor(private val api: __P__Api) : __P__Repository {
__ELSE__
internal class Network__P__Repository @Inject constructor(private val api: __P__Api) :
    __P__Repository {
__ELSE__
internal class Network__P__Repository @Inject constructor(
    private val api: __P__Api,
) : __P__Repository {
__END_FIT__

    private val fetched = MutableStateFlow<List<__P__Item>>(emptyList())

    override val items: Flow<List<__P__Item>> = fetched.asStateFlow()

    override suspend fun refresh(): AppResult<Unit> =
        api.items().map { dtos -> fetched.value = dtos.map { it.toDomain() } }

__FIT__
    override fun peek(id: String): __P__Item? = fetched.value.firstOrNull { it.id == id }
__ELSE__
    override fun peek(id: String): __P__Item? =
        fetched.value.firstOrNull { it.id == id }
__END_FIT__

    override suspend fun item(id: String): AppResult<__P__Item> =
        peek(id)?.let { AppResult.Success(it) } ?: api.item(id).map { it.toDomain() }
}
