package io.example.fit.data.abcd.local

import io.example.fit.core.database.abcd.AbcdEntity
import io.example.fit.data.abcd.api.model.AbcdItem
import io.example.fit.data.abcd.remote.dto.AbcdDto
import io.example.fit.data.abcd.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdDto.toEntity(position: Int): AbcdEntity = toDomain().let { item ->
    AbcdEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdEntity.toDomain(): AbcdItem = AbcdItem(id = id, title = title)
