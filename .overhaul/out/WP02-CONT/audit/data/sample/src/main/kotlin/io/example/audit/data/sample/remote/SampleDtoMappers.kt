package io.example.audit.data.sample.remote

import io.example.audit.data.sample.api.model.SampleItem
import io.example.audit.data.sample.remote.dto.SampleDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun SampleDto.toDomain(): SampleItem = SampleItem(
    id = id,
    title = title.trim(),
    body = body.trim(),
)
