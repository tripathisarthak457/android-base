package com.example.finalafter.crui.data.sample.remote

import com.example.finalafter.crui.data.sample.api.model.SampleItem
import com.example.finalafter.crui.data.sample.remote.dto.SampleDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun SampleDto.toDomain(): SampleItem = SampleItem(
    id = id,
    title = title.trim(),
    body = body.trim(),
)
