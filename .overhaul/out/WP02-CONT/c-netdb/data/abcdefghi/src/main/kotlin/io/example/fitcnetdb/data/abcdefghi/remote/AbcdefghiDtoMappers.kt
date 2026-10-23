package io.example.fitcnetdb.data.abcdefghi.remote

import io.example.fitcnetdb.data.abcdefghi.api.model.AbcdefghiItem
import io.example.fitcnetdb.data.abcdefghi.remote.dto.AbcdefghiDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcdefghiDto.toDomain(): AbcdefghiItem = AbcdefghiItem(id = id, title = title.trim())
