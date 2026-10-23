package io.example.fit.data.abcdefghijklmno.remote

import io.example.fit.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem
import io.example.fit.data.abcdefghijklmno.remote.dto.AbcdefghijklmnoDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnoDto.toDomain(): AbcdefghijklmnoItem =
    AbcdefghijklmnoItem(id = id, title = title.trim())
