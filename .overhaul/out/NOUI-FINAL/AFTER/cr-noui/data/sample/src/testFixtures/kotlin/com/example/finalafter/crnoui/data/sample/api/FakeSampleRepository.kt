package com.example.finalafter.crnoui.data.sample.api

import com.example.finalafter.crnoui.core.common.result.AppError
import com.example.finalafter.crnoui.core.common.result.AppResult
import com.example.finalafter.crnoui.data.sample.api.model.SampleItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [SampleRepository] whose server answers with [result]: each [refresh] stores its list, and an
 * item that is not stored yet comes from it as from the network. A failure leaves what is stored
 * alone. Set [result] between events to script a failure or a recovery; [loads] counts the list
 * requests, so a test can check that a screen does not load twice.
 */
class FakeSampleRepository(var result: AppResult<List<SampleItem>>) : SampleRepository {

    var loads = 0
        private set

    private val stored = MutableStateFlow<List<SampleItem>>(emptyList())

    override val items: Flow<List<SampleItem>> = stored

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

    override fun peek(id: Int): SampleItem? = stored.value.firstOrNull { it.id == id }

    override suspend fun item(id: Int): AppResult<SampleItem> {
        peek(id)?.let { return AppResult.Success(it) }
        return when (val current = result) {
            is AppResult.Success ->
                current.value.firstOrNull { it.id == id }?.let { AppResult.Success(it) }
                    ?: AppResult.Failure(AppError.Http(status = HTTP_NOT_FOUND))

            is AppResult.Failure -> current
        }
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
