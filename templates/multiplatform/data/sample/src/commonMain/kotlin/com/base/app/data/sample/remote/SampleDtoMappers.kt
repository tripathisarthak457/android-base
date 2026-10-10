package com.base.app.data.sample.remote

import com.base.app.data.sample.api.model.SampleItem
import com.base.app.data.sample.remote.dto.SampleDto

/** Trims what the wire sends, and names an untitled row rather than showing an empty one. */
internal fun SampleDto.toDomain(): SampleItem = SampleItem(
    id = id,
    title = title.trim().ifBlank { "Untitled" },
    body = body.trim(),
)
