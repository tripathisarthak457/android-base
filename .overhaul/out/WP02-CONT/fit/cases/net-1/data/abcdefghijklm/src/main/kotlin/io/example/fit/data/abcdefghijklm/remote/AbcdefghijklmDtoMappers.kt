package io.example.fit.data.abcdefghijklm.remote

import io.example.fit.data.abcdefghijklm.api.model.AbcdefghijklmItem
import io.example.fit.data.abcdefghijklm.remote.dto.AbcdefghijklmDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmDto.toDomain(): AbcdefghijklmItem =
    AbcdefghijklmItem(id = id, title = title.trim())
