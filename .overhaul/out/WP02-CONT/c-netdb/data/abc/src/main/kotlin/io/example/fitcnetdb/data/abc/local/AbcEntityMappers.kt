package io.example.fitcnetdb.data.abc.local

import io.example.fitcnetdb.core.database.abc.AbcEntity
import io.example.fitcnetdb.data.abc.api.model.AbcItem
import io.example.fitcnetdb.data.abc.remote.dto.AbcDto
import io.example.fitcnetdb.data.abc.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcDto.toEntity(position: Int): AbcEntity = toDomain().let { item ->
    AbcEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcEntity.toDomain(): AbcItem = AbcItem(id = id, title = title)
