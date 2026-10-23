package com.example.videoalone.data.home.api

import com.example.videoalone.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
class FakeHomeRepository(var result: AppResult<List<HomeItem>>) : HomeRepository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<HomeItem>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<HomeItem> = when (val current = result) {
        is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
        is AppResult.Failure -> current
    }
}
