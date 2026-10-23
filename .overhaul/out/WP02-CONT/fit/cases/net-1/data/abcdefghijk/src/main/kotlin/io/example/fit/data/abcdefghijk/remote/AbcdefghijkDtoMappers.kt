package io.example.fit.data.abcdefghijk.remote

import io.example.fit.data.abcdefghijk.api.model.AbcdefghijkItem
import io.example.fit.data.abcdefghijk.remote.dto.AbcdefghijkDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijkDto.toDomain(): AbcdefghijkItem =
    AbcdefghijkItem(id = id, title = title.trim())
