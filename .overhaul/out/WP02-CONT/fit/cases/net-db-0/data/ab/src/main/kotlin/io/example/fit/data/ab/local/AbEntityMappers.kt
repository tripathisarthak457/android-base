package io.example.fit.data.ab.local

import io.example.fit.core.database.ab.AbEntity
import io.example.fit.data.ab.api.model.AbItem
import io.example.fit.data.ab.remote.dto.AbDto
import io.example.fit.data.ab.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbDto.toEntity(position: Int): AbEntity = toDomain().let { item ->
    AbEntity(id = item.id, title = item.title, position = position)
}

internal fun AbEntity.toDomain(): AbItem = AbItem(id = id, title = title)
