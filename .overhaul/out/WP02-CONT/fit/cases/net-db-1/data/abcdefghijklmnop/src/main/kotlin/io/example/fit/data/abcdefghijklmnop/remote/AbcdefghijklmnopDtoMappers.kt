package io.example.fit.data.abcdefghijklmnop.remote

import io.example.fit.data.abcdefghijklmnop.api.model.AbcdefghijklmnopItem
import io.example.fit.data.abcdefghijklmnop.remote.dto.AbcdefghijklmnopDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopDto.toDomain(): AbcdefghijklmnopItem =
    AbcdefghijklmnopItem(id = id, title = title.trim())
