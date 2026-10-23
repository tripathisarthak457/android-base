package io.example.fit.data.abcdefghijklmn.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class AbcdefghijklmnDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String = "",
)
