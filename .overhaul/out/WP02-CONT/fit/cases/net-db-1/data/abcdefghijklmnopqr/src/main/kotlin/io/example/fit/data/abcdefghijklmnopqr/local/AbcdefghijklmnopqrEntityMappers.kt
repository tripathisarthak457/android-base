package io.example.fit.data.abcdefghijklmnopqr.local

import io.example.fit.core.database.abcdefghijklmnopqr.AbcdefghijklmnopqrEntity
import io.example.fit.data.abcdefghijklmnopqr.api.model.AbcdefghijklmnopqrItem
import io.example.fit.data.abcdefghijklmnopqr.remote.dto.AbcdefghijklmnopqrDto
import io.example.fit.data.abcdefghijklmnopqr.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrDto.toEntity(position: Int): AbcdefghijklmnopqrEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrEntity.toDomain(): AbcdefghijklmnopqrItem =
    AbcdefghijklmnopqrItem(id = id, title = title)
