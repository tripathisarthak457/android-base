package io.example.fit.data.abcde.remote

import io.example.fit.data.abcde.api.model.AbcdeItem
import io.example.fit.data.abcde.remote.dto.AbcdeDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdeDto.toDomain(): AbcdeItem = AbcdeItem(id = id, title = title.trim())
