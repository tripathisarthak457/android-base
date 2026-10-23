package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote

import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.dto.AbcdefghijklmnopqrstuvwxyzabDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnopqrstuvwxyzabDto.toDomain(): AbcdefghijklmnopqrstuvwxyzabItem =
    AbcdefghijklmnopqrstuvwxyzabItem(id = id, title = title.trim())
