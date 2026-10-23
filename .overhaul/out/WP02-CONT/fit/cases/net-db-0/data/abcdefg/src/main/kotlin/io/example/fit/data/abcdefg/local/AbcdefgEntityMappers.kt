package io.example.fit.data.abcdefg.local

import io.example.fit.core.database.abcdefg.AbcdefgEntity
import io.example.fit.data.abcdefg.api.model.AbcdefgItem
import io.example.fit.data.abcdefg.remote.dto.AbcdefgDto
import io.example.fit.data.abcdefg.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefgDto.toEntity(position: Int): AbcdefgEntity = toDomain().let { item ->
    AbcdefgEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefgEntity.toDomain(): AbcdefgItem = AbcdefgItem(id = id, title = title)
