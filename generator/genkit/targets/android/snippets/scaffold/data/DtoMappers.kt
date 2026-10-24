package __PKG__.data.__NAME__.remote

import __PKG__.data.__NAME__.api.model.__P__Item
import __PKG__.data.__NAME__.remote.dto.__P__Dto

/**
 * Trims what the wire sends. A blank title stays blank: wording a fallback is the screen's job, in
 * the person's language.
 */
__FIT__
internal fun __P__Dto.toDomain(): __P__Item = __P__Item(id = id, title = title.trim())
__ELSE__
internal fun __P__Dto.toDomain(): __P__Item =
    __P__Item(id = id, title = title.trim())
__END_FIT__
