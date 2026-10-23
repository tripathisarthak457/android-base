package io.example.fit.data.abcdefghijklmnopqrs.local

import io.example.fit.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsEntity
import io.example.fit.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem
import io.example.fit.data.abcdefghijklmnopqrs.remote.dto.AbcdefghijklmnopqrsDto
import io.example.fit.data.abcdefghijklmnopqrs.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrsDto.toEntity(position: Int): AbcdefghijklmnopqrsEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrsEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrsEntity.toDomain(): AbcdefghijklmnopqrsItem =
    AbcdefghijklmnopqrsItem(id = id, title = title)
