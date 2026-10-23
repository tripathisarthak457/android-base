package io.example.fit.data.abcdefghijkl.remote

import io.example.fit.data.abcdefghijkl.api.model.AbcdefghijklItem
import io.example.fit.data.abcdefghijkl.remote.dto.AbcdefghijklDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklDto.toDomain(): AbcdefghijklItem =
    AbcdefghijklItem(id = id, title = title.trim())
