package com.example.modulesmodular.data.trips.remote

import com.example.modulesmodular.data.trips.api.TripsItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class TripsDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)

internal fun TripsDto.toDomain(): TripsItem = TripsItem(id = id, title = title.trim())
