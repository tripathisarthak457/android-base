package io.example.fit.data.abcdefghijklmnopq.local

import io.example.fit.core.database.abcdefghijklmnopq.AbcdefghijklmnopqEntity
import io.example.fit.data.abcdefghijklmnopq.api.model.AbcdefghijklmnopqItem
import io.example.fit.data.abcdefghijklmnopq.remote.dto.AbcdefghijklmnopqDto
import io.example.fit.data.abcdefghijklmnopq.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqDto.toEntity(position: Int): AbcdefghijklmnopqEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqEntity.toDomain(): AbcdefghijklmnopqItem =
    AbcdefghijklmnopqItem(id = id, title = title)
