package io.example.fit.data.abcdefghijklmnopqrstuvwxyz.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyz.AbcdefghijklmnopqrstuvwxyzEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.api.model.AbcdefghijklmnopqrstuvwxyzItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.dto.AbcdefghijklmnopqrstuvwxyzDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxyz.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzItem =
    AbcdefghijklmnopqrstuvwxyzItem(id = id, title = title)
