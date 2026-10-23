package io.example.fit.data.abcdefghijklmnopqrstuvwxyza.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyza.AbcdefghijklmnopqrstuvwxyzaEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.api.model.AbcdefghijklmnopqrstuvwxyzaItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote.dto.AbcdefghijklmnopqrstuvwxyzaDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxyza.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzaDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzaEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzaEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzaEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzaItem =
    AbcdefghijklmnopqrstuvwxyzaItem(id = id, title = title)
