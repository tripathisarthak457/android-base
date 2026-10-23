package io.example.fit.data.abcdefghijklmnopq.remote

import io.example.fit.data.abcdefghijklmnopq.api.model.AbcdefghijklmnopqItem
import io.example.fit.data.abcdefghijklmnopq.remote.dto.AbcdefghijklmnopqDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqDto.toDomain(): AbcdefghijklmnopqItem =
    AbcdefghijklmnopqItem(id = id, title = title.trim())
