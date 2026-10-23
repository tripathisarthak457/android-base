package io.example.fit.data.ab.remote

import io.example.fit.data.ab.api.model.AbItem
import io.example.fit.data.ab.remote.dto.AbDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbDto.toDomain(): AbItem = AbItem(id = id, title = title.trim())
