package io.example.fit.data.abcdefghijklmnopqrs.remote

import io.example.fit.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem
import io.example.fit.data.abcdefghijklmnopqrs.remote.dto.AbcdefghijklmnopqrsDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrsDto.toDomain(): AbcdefghijklmnopqrsItem =
    AbcdefghijklmnopqrsItem(id = id, title = title.trim())
