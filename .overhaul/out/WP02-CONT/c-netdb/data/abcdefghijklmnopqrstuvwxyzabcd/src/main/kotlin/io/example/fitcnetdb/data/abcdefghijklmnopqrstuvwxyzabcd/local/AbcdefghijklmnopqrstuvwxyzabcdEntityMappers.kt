package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.local

import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvwxyzabcd.AbcdefghijklmnopqrstuvwxyzabcdEntity
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.api.model.AbcdefghijklmnopqrstuvwxyzabcdItem
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote.dto.AbcdefghijklmnopqrstuvwxyzabcdDto
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvwxyzabcd.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzabcdDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzabcdEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzabcdEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzabcdEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzabcdItem =
    AbcdefghijklmnopqrstuvwxyzabcdItem(id = id, title = title)
