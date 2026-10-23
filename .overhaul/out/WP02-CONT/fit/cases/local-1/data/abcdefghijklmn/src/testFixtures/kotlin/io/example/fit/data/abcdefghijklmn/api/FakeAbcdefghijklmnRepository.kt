package io.example.fit.data.abcdefghijklmn.api

import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps the list in memory and answers [refresh] with [result], counting the loads so a test can
 * check that a screen does not load twice. Set [result] between events to script a failure or a
 * recovery, and call [write] for rows that arrive without a refresh.
 */
class FakeAbcdefghijklmnRepository(var result: AppResult<List<AbcdefghijklmnItem>>) :
    AbcdefghijklmnRepository {

    var loads = 0
        private set

    private val stored = MutableStateFlow<List<AbcdefghijklmnItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnItem>> = stored

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

    override fun peek(id: String): AbcdefghijklmnItem? = stored.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnItem> =
        peek(id)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Http(status = 404))

    /** Rows that reach the list without a refresh: another screen's write, a sync, a push. */
    fun write(items: List<AbcdefghijklmnItem>) {
        stored.value = items
    }
}
