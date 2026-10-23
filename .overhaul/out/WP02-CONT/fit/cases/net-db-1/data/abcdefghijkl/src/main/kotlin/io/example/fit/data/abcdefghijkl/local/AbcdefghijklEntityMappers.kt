package io.example.fit.data.abcdefghijkl.local

import io.example.fit.core.database.abcdefghijkl.AbcdefghijklEntity
import io.example.fit.data.abcdefghijkl.api.model.AbcdefghijklItem
import io.example.fit.data.abcdefghijkl.remote.dto.AbcdefghijklDto
import io.example.fit.data.abcdefghijkl.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklDto.toEntity(position: Int): AbcdefghijklEntity = toDomain().let { item ->
    AbcdefghijklEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklEntity.toDomain(): AbcdefghijklItem =
    AbcdefghijklItem(id = id, title = title)
