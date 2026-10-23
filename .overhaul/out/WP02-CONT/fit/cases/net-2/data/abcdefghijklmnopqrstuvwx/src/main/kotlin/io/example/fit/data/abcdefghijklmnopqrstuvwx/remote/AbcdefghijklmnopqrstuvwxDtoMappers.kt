package io.example.fit.data.abcdefghijklmnopqrstuvwx.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.model.AbcdefghijklmnopqrstuvwxItem
import io.example.fit.data.abcdefghijklmnopqrstuvwx.remote.dto.AbcdefghijklmnopqrstuvwxDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxDto.toDomain(): AbcdefghijklmnopqrstuvwxItem =
    AbcdefghijklmnopqrstuvwxItem(id = id, title = title.trim())
