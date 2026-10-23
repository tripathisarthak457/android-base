package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote

import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzabcdDto.toDomain(): AbcdefghijklmnopqrstuvwxyzabcdItem =
    AbcdefghijklmnopqrstuvwxyzabcdItem(id = id, title = title.trim())
