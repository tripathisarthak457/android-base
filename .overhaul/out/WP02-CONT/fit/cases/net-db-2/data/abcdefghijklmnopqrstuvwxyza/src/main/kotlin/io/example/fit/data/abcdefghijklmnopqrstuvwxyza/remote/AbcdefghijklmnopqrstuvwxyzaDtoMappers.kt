package io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.model.AbcdefghijklmnopqrstuvwxyzaItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote.dto.AbcdefghijklmnopqrstuvwxyzaDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzaDto.toDomain(): AbcdefghijklmnopqrstuvwxyzaItem =
    AbcdefghijklmnopqrstuvwxyzaItem(id = id, title = title.trim())
