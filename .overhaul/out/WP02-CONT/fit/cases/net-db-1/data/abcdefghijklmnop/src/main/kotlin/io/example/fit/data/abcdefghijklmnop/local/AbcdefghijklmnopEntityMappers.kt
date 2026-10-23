package io.example.fit.data.abcdefghijklmnop.local

import io.example.fit.core.database.abcdefghijklmnop.AbcdefghijklmnopEntity
import io.example.fit.data.abcdefghijklmnop.api.model.AbcdefghijklmnopItem
import io.example.fit.data.abcdefghijklmnop.remote.dto.AbcdefghijklmnopDto
import io.example.fit.data.abcdefghijklmnop.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopDto.toEntity(position: Int): AbcdefghijklmnopEntity =
    toDomain().let { item ->
        AbcdefghijklmnopEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopEntity.toDomain(): AbcdefghijklmnopItem =
    AbcdefghijklmnopItem(id = id, title = title)
