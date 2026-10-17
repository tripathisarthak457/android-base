package com.example.modulescompact.data.trips.api

import com.example.modulescompact.core.common.result.AppResult

/**
 * Everything the app can ask about trips.
 * An interface, so ViewModel tests use a fake.
 */
interface TripsRepository {

    suspend fun items(forceRefresh: Boolean = false): AppResult<List<TripsItem>>

    suspend fun item(id: String): AppResult<TripsItem>
}
