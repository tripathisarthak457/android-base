package com.example.modulesmodular.data.trips

import com.example.modulesmodular.core.common.result.AppResult
import com.example.modulesmodular.core.common.result.map
import com.example.modulesmodular.core.network.NetworkClient
import com.example.modulesmodular.core.network.get
import com.example.modulesmodular.core.network.model.CachePolicy
import com.example.modulesmodular.data.trips.api.TripsItem
import com.example.modulesmodular.data.trips.api.TripsRepository
import com.example.modulesmodular.data.trips.remote.TripsDto
import com.example.modulesmodular.data.trips.remote.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DefaultTripsRepository @Inject constructor(
    private val networkClient: NetworkClient,
) : TripsRepository {

    /** Cached, so reopening the screen or going offline shows the last answer. */
    override suspend fun items(forceRefresh: Boolean): AppResult<List<TripsItem>> =
        networkClient.get<List<TripsDto>>(
            path = PATH,
            cache = CachePolicy.Enabled(
                key = PATH,
                maxAgeMillis = CACHE_MAX_AGE_MILLIS,
                forceRefresh = forceRefresh,
            ),
        ).map { dtos -> dtos.map(TripsDto::toDomain) }

    override suspend fun item(id: String): AppResult<TripsItem> = networkClient.get<TripsDto>(
        path = "$PATH/$id",
        cache = CachePolicy.Enabled(key = "$PATH/$id", maxAgeMillis = CACHE_MAX_AGE_MILLIS),
    ).map(TripsDto::toDomain)

    private companion object {
        const val PATH = "trips"
        const val CACHE_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
