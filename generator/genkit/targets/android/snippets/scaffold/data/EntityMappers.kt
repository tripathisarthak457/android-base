package __PKG__.data.__NAME__.local

import __PKG__.core.database.__NAME__.__P__Entity
import __PKG__.data.__NAME__.api.model.__P__Item
import __PKG__.data.__NAME__.remote.dto.__P__Dto
import __PKG__.data.__NAME__.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
__FIT__
internal fun __P__Dto.toEntity(position: Int): __P__Entity = toDomain().let { item ->
    __P__Entity(id = item.id, title = item.title, position = position)
}
__ELSE__
internal fun __P__Dto.toEntity(position: Int): __P__Entity =
    toDomain().let { item ->
        __P__Entity(id = item.id, title = item.title, position = position)
    }
__ELSE__
internal fun __P__Dto.toEntity(
    position: Int,
): __P__Entity = toDomain().let { item ->
    __P__Entity(id = item.id, title = item.title, position = position)
}
__END_FIT__

__FIT__
internal fun __P__Entity.toDomain(): __P__Item = __P__Item(id = id, title = title)
__ELSE__
internal fun __P__Entity.toDomain(): __P__Item =
    __P__Item(id = id, title = title)
__END_FIT__
