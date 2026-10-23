package io.example.fit.data.abcdefghij.local

import io.example.fit.core.database.abcdefghij.AbcdefghijEntity
import io.example.fit.data.abcdefghij.api.model.AbcdefghijItem
import io.example.fit.data.abcdefghij.remote.dto.AbcdefghijDto
import io.example.fit.data.abcdefghij.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijDto.toEntity(position: Int): AbcdefghijEntity = toDomain().let { item ->
    AbcdefghijEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghijEntity.toDomain(): AbcdefghijItem = AbcdefghijItem(id = id, title = title)
