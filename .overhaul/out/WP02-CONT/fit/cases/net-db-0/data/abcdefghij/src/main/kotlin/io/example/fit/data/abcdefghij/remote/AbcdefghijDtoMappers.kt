package io.example.fit.data.abcdefghij.remote

import io.example.fit.data.abcdefghij.api.model.AbcdefghijItem
import io.example.fit.data.abcdefghij.remote.dto.AbcdefghijDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijDto.toDomain(): AbcdefghijItem =
    AbcdefghijItem(id = id, title = title.trim())
