package io.example.fit.data.abcdefghijklmnopqr.remote

import io.example.fit.data.abcdefghijklmnopqr.api.model.AbcdefghijklmnopqrItem
import io.example.fit.data.abcdefghijklmnopqr.remote.dto.AbcdefghijklmnopqrDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrDto.toDomain(): AbcdefghijklmnopqrItem =
    AbcdefghijklmnopqrItem(id = id, title = title.trim())
