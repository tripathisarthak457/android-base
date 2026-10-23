package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.model.AbcdefghijklmnopqrstuvwxyzabcItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote.dto.AbcdefghijklmnopqrstuvwxyzabcDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzabcDto.toDomain(): AbcdefghijklmnopqrstuvwxyzabcItem =
    AbcdefghijklmnopqrstuvwxyzabcItem(id = id, title = title.trim())
