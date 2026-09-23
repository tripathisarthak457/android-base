package com.base.app.data.sample.remote

import com.base.app.data.sample.SampleItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The wire format, exactly as the backend sends it. */
@Serializable
data class SampleDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
)

/**
 * DTO to domain. A free function rather than a method on the DTO, so the domain model has no idea
 * the wire format exists and the mapping is trivially testable — see `SampleMapperTest`.
 */
fun SampleDto.toDomain(): SampleItem = SampleItem(
    id = id,
    title = title.trim().ifBlank { "Untitled" },
    body = body.trim(),
)

fun List<SampleDto>.toDomain(): List<SampleItem> = map(SampleDto::toDomain)
