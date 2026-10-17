package com.example.modulescompact.data.trips.api

import com.example.modulescompact.core.common.result.AppResult

/**
 * Answers with [result] and counts the list loads, so a test can check that a screen does not load
 * twice. Set [result] between events to script a failure or a recovery.
 */
class FakeTripsRepository(var result: AppResult<List<TripsItem>>) : TripsRepository {

    var loads = 0
        private set

    override suspend fun items(forceRefresh: Boolean): AppResult<List<TripsItem>> {
        loads++
        return result
    }

    override suspend fun item(id: String): AppResult<TripsItem> = when (val current = result) {
        is AppResult.Success -> AppResult.Success(current.value.first { it.id == id })
        is AppResult.Failure -> current
    }
}
