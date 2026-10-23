package io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzabcd.AbcdefghijklmnopqrstuvwxyzabcdEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzabcd.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzabcdDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzabcdEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzabcdEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzabcdEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzabcdItem =
    AbcdefghijklmnopqrstuvwxyzabcdItem(id = id, title = title)
