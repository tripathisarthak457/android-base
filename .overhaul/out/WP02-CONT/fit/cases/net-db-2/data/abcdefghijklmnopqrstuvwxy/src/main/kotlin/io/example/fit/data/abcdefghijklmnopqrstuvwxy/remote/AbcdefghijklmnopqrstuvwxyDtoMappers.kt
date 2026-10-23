package io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.dto.AbcdefghijklmnopqrstuvwxyDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyDto.toDomain(): AbcdefghijklmnopqrstuvwxyItem =
    AbcdefghijklmnopqrstuvwxyItem(id = id, title = title.trim())
