package io.example.fit.data.abcdefghijklmnopqrst.local

import io.example.fit.core.database.abcdefghijklmnopqrst.AbcdefghijklmnopqrstEntity
import io.example.fit.data.abcdefghijklmnopqrst.api.model.AbcdefghijklmnopqrstItem
import io.example.fit.data.abcdefghijklmnopqrst.remote.dto.AbcdefghijklmnopqrstDto
import io.example.fit.data.abcdefghijklmnopqrst.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstDto.toEntity(position: Int): AbcdefghijklmnopqrstEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstEntity.toDomain(): AbcdefghijklmnopqrstItem =
    AbcdefghijklmnopqrstItem(id = id, title = title)
