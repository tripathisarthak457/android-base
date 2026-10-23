package io.example.fit.data.abcdefghijklm.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class AbcdefghijklmDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)
