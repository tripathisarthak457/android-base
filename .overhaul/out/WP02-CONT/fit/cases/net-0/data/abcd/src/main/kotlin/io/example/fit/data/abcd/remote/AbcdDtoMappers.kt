package io.example.fit.data.abcd.remote

import io.example.fit.data.abcd.api.model.AbcdItem
import io.example.fit.data.abcd.remote.dto.AbcdDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdDto.toDomain(): AbcdItem = AbcdItem(id = id, title = title.trim())
