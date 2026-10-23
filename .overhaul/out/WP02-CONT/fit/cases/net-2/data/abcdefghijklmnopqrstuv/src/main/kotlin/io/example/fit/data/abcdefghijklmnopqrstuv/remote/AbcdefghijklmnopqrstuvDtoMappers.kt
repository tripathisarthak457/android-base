package io.example.fit.data.abcdefghijklmnopqrstuv.remote

import io.example.fit.data.abcdefghijklmnopqrstuv.api.model.AbcdefghijklmnopqrstuvItem
import io.example.fit.data.abcdefghijklmnopqrstuv.remote.dto.AbcdefghijklmnopqrstuvDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvDto.toDomain(): AbcdefghijklmnopqrstuvItem =
    AbcdefghijklmnopqrstuvItem(id = id, title = title.trim())
