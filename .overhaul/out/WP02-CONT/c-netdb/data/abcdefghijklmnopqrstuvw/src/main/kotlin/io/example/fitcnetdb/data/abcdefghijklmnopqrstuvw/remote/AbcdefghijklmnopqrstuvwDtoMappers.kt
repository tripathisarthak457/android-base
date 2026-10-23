package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote

import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote.dto.AbcdefghijklmnopqrstuvwDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwDto.toDomain(): AbcdefghijklmnopqrstuvwItem =
    AbcdefghijklmnopqrstuvwItem(id = id, title = title.trim())
