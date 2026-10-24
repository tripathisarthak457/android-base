package io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.remote

import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.audit.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzabcdDto.toDomain(): AbcdefghijklmnopqrstuvwxyzabcdItem =
    AbcdefghijklmnopqrstuvwxyzabcdItem(id = id, title = title.trim())
