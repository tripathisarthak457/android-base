package io.example.fit.data.abcdefghijklmnopqrst.remote

import io.example.fit.data.abcdefghijklmnopqrst.api.model.AbcdefghijklmnopqrstItem
import io.example.fit.data.abcdefghijklmnopqrst.remote.dto.AbcdefghijklmnopqrstDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstDto.toDomain(): AbcdefghijklmnopqrstItem =
    AbcdefghijklmnopqrstItem(id = id, title = title.trim())
