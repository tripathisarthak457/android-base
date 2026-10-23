package io.example.fit.data.abcdefghijklmnopqrstu.local

import io.example.fit.core.database.abcdefghijklmnopqrstu.AbcdefghijklmnopqrstuEntity
import io.example.fit.data.abcdefghijklmnopqrstu.api.model.AbcdefghijklmnopqrstuItem
import io.example.fit.data.abcdefghijklmnopqrstu.remote.dto.AbcdefghijklmnopqrstuDto
import io.example.fit.data.abcdefghijklmnopqrstu.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuDto.toEntity(position: Int): AbcdefghijklmnopqrstuEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuEntity.toDomain(): AbcdefghijklmnopqrstuItem =
    AbcdefghijklmnopqrstuItem(id = id, title = title)
