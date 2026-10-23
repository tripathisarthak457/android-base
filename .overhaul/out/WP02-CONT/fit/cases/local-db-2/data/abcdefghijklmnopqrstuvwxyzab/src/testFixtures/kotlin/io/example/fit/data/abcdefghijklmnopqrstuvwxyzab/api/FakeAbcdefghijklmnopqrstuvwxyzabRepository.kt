package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api

import io.example.fit.core.common.result.AppError
import io.example.fit.core.common.result.AppResult
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Keeps the list in memory and answers [refresh] with [result], counting the loads so a test can
 * check that a screen does not load twice. Set [result] between events to script a failure or a
 * recovery, and call [write] for rows that arrive without a refresh.
 */
class FakeAbcdefghijklmnopqrstuvwxyzabRepository(
    var result: AppResult<List<AbcdefghijklmnopqrstuvwxyzabItem>>,
) : AbcdefghijklmnopqrstuvwxyzabRepository {

    var loads = 0
        private set

    private val stored = MutableStateFlow<List<AbcdefghijklmnopqrstuvwxyzabItem>>(emptyList())

    override val items: Flow<List<AbcdefghijklmnopqrstuvwxyzabItem>> = stored

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

    override fun peek(id: String): AbcdefghijklmnopqrstuvwxyzabItem? =
        stored.value.firstOrNull { it.id == id }

    override suspend fun item(id: String): AppResult<AbcdefghijklmnopqrstuvwxyzabItem> =
        peek(id)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Http(status = 404))

    /** Rows that reach the list without a refresh: another screen's write, a sync, a push. */
    fun write(items: List<AbcdefghijklmnopqrstuvwxyzabItem>) {
        stored.value = items
    }
}
