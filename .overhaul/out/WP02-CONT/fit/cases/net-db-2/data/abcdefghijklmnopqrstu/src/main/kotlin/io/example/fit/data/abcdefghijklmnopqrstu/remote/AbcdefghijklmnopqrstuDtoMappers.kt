package io.example.fit.data.abcdefghijklmnopqrstu.remote

import io.example.fit.data.abcdefghijklmnopqrstu.api.model.AbcdefghijklmnopqrstuItem
import io.example.fit.data.abcdefghijklmnopqrstu.remote.dto.AbcdefghijklmnopqrstuDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuDto.toDomain(): AbcdefghijklmnopqrstuItem =
    AbcdefghijklmnopqrstuItem(id = id, title = title.trim())
