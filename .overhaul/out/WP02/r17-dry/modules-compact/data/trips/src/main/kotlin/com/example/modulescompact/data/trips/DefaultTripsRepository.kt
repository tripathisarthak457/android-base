package com.example.modulescompact.data.trips

import com.example.modulescompact.core.common.result.AppResult
import com.example.modulescompact.core.common.result.map
import com.example.modulescompact.core.network.NetworkClient
import com.example.modulescompact.core.network.get
import com.example.modulescompact.core.network.model.CachePolicy
import com.example.modulescompact.data.trips.api.TripsItem
import com.example.modulescompact.data.trips.api.TripsRepository
import com.example.modulescompact.data.trips.remote.TripsDto
import com.example.modulescompact.data.trips.remote.toDomain
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
