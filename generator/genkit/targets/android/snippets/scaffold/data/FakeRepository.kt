package __PKG__.data.__NAME__.api

import __PKG__.core.common.result.AppError
import __PKG__.core.common.result.AppResult
import __PKG__.data.__NAME__.api.model.__P__Item
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps the list in memory and answers [refresh] with [result], counting the loads so a test can
 * check that a screen does not load twice. Set [result] between events to script a failure or a
 * recovery, and call [write] for rows that arrive without a refresh.
 */
__FIT__
class Fake__P__Repository(var result: AppResult<List<__P__Item>>) : __P__Repository {
__ELSE__
class Fake__P__Repository(var result: AppResult<List<__P__Item>>) :
    __P__Repository {
__ELSE__
class Fake__P__Repository(
    var result: AppResult<List<__P__Item>>,
) : __P__Repository {
__END_FIT__

    var loads = 0
        private set

    private val stored = MutableStateFlow<List<__P__Item>>(emptyList())

    override val items: Flow<List<__P__Item>> = stored

    override suspend fun refresh(): AppResult<Unit> {
        loads++
        return when (val current = result) {
            is AppResult.Success -> {
                stored.value = current.value
                AppResult.Success(Unit)
            }

            is AppResult.Failure -> current
        }
    }

__FIT__
    override fun peek(id: String): __P__Item? = stored.value.firstOrNull { it.id == id }
__ELSE__
    override fun peek(id: String): __P__Item? =
        stored.value.firstOrNull { it.id == id }
__END_FIT__

    override suspend fun item(id: String): AppResult<__P__Item> =
        peek(id)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Http(status = 404))

    /** Rows that reach the list without a refresh: another screen's write, a sync, a push. */
    fun write(items: List<__P__Item>) {
        stored.value = items
    }
}
