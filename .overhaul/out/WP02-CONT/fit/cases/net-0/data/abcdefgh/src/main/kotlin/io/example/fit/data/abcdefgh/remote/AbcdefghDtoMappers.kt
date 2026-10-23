package io.example.fit.data.abcdefgh.remote

import io.example.fit.data.abcdefgh.api.model.AbcdefghItem
import io.example.fit.data.abcdefgh.remote.dto.AbcdefghDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghDto.toDomain(): AbcdefghItem = AbcdefghItem(id = id, title = title.trim())
