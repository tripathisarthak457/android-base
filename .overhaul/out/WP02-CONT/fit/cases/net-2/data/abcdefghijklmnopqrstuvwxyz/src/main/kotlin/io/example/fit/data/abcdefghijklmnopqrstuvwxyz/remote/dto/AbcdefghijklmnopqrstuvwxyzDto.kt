package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class AbcdefghijklmnopqrstuvwxyzDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)
