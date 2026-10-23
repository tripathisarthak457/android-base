package io.example.fit.data.abcdefghijklmnopqrstuvw.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwEntity
import io.example.fit.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem
import io.example.fit.data.abcdefghijklmnopqrstuvw.remote.dto.AbcdefghijklmnopqrstuvwDto
import io.example.fit.data.abcdefghijklmnopqrstuvw.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwDto.toEntity(position: Int): AbcdefghijklmnopqrstuvwEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuvwEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuvwEntity.toDomain(): AbcdefghijklmnopqrstuvwItem =
    AbcdefghijklmnopqrstuvwItem(id = id, title = title)
