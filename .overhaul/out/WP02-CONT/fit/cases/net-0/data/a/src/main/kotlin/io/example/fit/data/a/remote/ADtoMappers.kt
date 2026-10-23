package io.example.fit.data.a.remote

import io.example.fit.data.a.api.model.AItem
import io.example.fit.data.a.remote.dto.ADto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun ADto.toDomain(): AItem = AItem(id = id, title = title.trim())
