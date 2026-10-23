package io.example.fit.data.abcdef.remote

import io.example.fit.data.abcdef.api.model.AbcdefItem
import io.example.fit.data.abcdef.remote.dto.AbcdefDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefDto.toDomain(): AbcdefItem = AbcdefItem(id = id, title = title.trim())
