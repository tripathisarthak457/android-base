package io.example.fit.data.abcdef.local

import io.example.fit.core.database.abcdef.AbcdefEntity
import io.example.fit.data.abcdef.api.model.AbcdefItem
import io.example.fit.data.abcdef.remote.dto.AbcdefDto
import io.example.fit.data.abcdef.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefDto.toEntity(position: Int): AbcdefEntity = toDomain().let { item ->
    AbcdefEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefEntity.toDomain(): AbcdefItem = AbcdefItem(id = id, title = title)
