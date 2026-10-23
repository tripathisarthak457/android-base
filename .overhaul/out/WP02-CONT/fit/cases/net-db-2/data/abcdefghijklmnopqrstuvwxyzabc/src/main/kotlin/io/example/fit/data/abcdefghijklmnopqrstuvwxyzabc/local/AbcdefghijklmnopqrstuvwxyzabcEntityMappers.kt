package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzabc.AbcdefghijklmnopqrstuvwxyzabcEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.api.model.AbcdefghijklmnopqrstuvwxyzabcItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote.dto.AbcdefghijklmnopqrstuvwxyzabcDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabc.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzabcDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzabcEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzabcEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzabcEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzabcItem =
    AbcdefghijklmnopqrstuvwxyzabcItem(id = id, title = title)
