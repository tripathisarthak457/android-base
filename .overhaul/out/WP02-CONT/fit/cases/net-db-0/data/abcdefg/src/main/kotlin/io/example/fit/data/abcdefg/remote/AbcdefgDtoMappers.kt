package io.example.fit.data.abcdefg.remote

import io.example.fit.data.abcdefg.api.model.AbcdefgItem
import io.example.fit.data.abcdefg.remote.dto.AbcdefgDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefgDto.toDomain(): AbcdefgItem = AbcdefgItem(id = id, title = title.trim())
