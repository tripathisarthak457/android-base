package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzabcdDto.toDomain(): AbcdefghijklmnopqrstuvwxyzabcdItem =
    AbcdefghijklmnopqrstuvwxyzabcdItem(id = id, title = title.trim())
