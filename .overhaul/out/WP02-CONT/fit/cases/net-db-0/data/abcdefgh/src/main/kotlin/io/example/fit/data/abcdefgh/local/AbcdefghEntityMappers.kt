package io.example.fit.data.abcdefgh.local

import io.example.fit.core.database.abcdefgh.AbcdefghEntity
import io.example.fit.data.abcdefgh.api.model.AbcdefghItem
import io.example.fit.data.abcdefgh.remote.dto.AbcdefghDto
import io.example.fit.data.abcdefgh.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghDto.toEntity(position: Int): AbcdefghEntity = toDomain().let { item ->
    AbcdefghEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghEntity.toDomain(): AbcdefghItem = AbcdefghItem(id = id, title = title)
