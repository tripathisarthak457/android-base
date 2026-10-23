package io.example.fit.data.a.local

import io.example.fit.core.database.a.AEntity
import io.example.fit.data.a.api.model.AItem
import io.example.fit.data.a.remote.dto.ADto
import io.example.fit.data.a.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun ADto.toEntity(position: Int): AEntity = toDomain().let { item ->
    AEntity(id = item.id, title = item.title, position = position)
}

internal fun AEntity.toDomain(): AItem = AItem(id = id, title = title)
