package io.example.fit.data.abcdefghijk.local

import io.example.fit.core.database.abcdefghijk.AbcdefghijkEntity
import io.example.fit.data.abcdefghijk.api.model.AbcdefghijkItem
import io.example.fit.data.abcdefghijk.remote.dto.AbcdefghijkDto
import io.example.fit.data.abcdefghijk.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijkDto.toEntity(position: Int): AbcdefghijkEntity = toDomain().let { item ->
    AbcdefghijkEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijkEntity.toDomain(): AbcdefghijkItem = AbcdefghijkItem(id = id, title = title)
