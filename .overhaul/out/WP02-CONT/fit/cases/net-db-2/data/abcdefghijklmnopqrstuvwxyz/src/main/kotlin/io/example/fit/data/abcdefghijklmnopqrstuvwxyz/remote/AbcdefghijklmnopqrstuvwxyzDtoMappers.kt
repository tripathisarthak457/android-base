package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.model.AbcdefghijklmnopqrstuvwxyzItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.dto.AbcdefghijklmnopqrstuvwxyzDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzDto.toDomain(): AbcdefghijklmnopqrstuvwxyzItem =
    AbcdefghijklmnopqrstuvwxyzItem(id = id, title = title.trim())
