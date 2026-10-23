package io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxyzab.AbcdefghijklmnopqrstuvwxyzabEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.api.model.AbcdefghijklmnopqrstuvwxyzabItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.dto.AbcdefghijklmnopqrstuvwxyzabDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxyzab.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyzabDto.toEntity(
    position: Int,
): AbcdefghijklmnopqrstuvwxyzabEntity = toDomain().let { item ->
    AbcdefghijklmnopqrstuvwxyzabEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijklmnopqrstuvwxyzabEntity.toDomain(): AbcdefghijklmnopqrstuvwxyzabItem =
    AbcdefghijklmnopqrstuvwxyzabItem(id = id, title = title)
