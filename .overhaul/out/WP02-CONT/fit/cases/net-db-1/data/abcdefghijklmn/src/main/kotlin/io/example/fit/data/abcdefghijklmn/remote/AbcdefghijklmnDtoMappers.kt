package io.example.fit.data.abcdefghijklmn.remote

import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem
import io.example.fit.data.abcdefghijklmn.remote.dto.AbcdefghijklmnDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghijklmnDto.toDomain(): AbcdefghijklmnItem =
    AbcdefghijklmnItem(id = id, title = title.trim())
