package io.example.fitcnet.data.abc.remote

import io.example.fitcnet.data.abc.api.model.AbcItem
import io.example.fitcnet.data.abc.remote.dto.AbcDto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
internal fun AbcDto.toDomain(): AbcItem = AbcItem(id = id, title = title.trim())
