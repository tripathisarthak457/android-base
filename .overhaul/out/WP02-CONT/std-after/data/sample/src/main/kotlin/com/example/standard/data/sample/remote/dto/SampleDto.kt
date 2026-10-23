package com.example.standard.data.sample.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
internal data class SampleDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
)
