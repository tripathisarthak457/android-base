package io.example.fitcnetdb.data.abcdefghijklmnopqrs.local

import io.example.fitcnetdb.core.database.abcdefghijklmnopqrs.AbcdefghijklmnopqrsEntity
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.api.model.AbcdefghijklmnopqrsItem
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.remote.dto.AbcdefghijklmnopqrsDto
import io.example.fitcnetdb.data.abcdefghijklmnopqrs.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrsDto.toEntity(position: Int): AbcdefghijklmnopqrsEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrsEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrsEntity.toDomain(): AbcdefghijklmnopqrsItem =
    AbcdefghijklmnopqrsItem(id = id, title = title)
