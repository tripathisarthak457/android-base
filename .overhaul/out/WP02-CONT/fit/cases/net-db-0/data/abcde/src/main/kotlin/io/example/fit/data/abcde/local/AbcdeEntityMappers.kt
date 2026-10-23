package io.example.fit.data.abcde.local

import io.example.fit.core.database.abcde.AbcdeEntity
import io.example.fit.data.abcde.api.model.AbcdeItem
import io.example.fit.data.abcde.remote.dto.AbcdeDto
import io.example.fit.data.abcde.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdeDto.toEntity(position: Int): AbcdeEntity = toDomain().let { item ->
    AbcdeEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdeEntity.toDomain(): AbcdeItem = AbcdeItem(id = id, title = title)
