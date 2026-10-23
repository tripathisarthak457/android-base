package io.example.fit.data.abcdefghijklmnopqrstuv.local

import io.example.fit.core.database.abcdefghijklmnopqrstuv.AbcdefghijklmnopqrstuvEntity
import io.example.fit.data.abcdefghijklmnopqrstuv.api.model.AbcdefghijklmnopqrstuvItem
import io.example.fit.data.abcdefghijklmnopqrstuv.remote.dto.AbcdefghijklmnopqrstuvDto
import io.example.fit.data.abcdefghijklmnopqrstuv.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvDto.toEntity(position: Int): AbcdefghijklmnopqrstuvEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuvEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuvEntity.toDomain(): AbcdefghijklmnopqrstuvItem =
    AbcdefghijklmnopqrstuvItem(id = id, title = title)
