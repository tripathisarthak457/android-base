package io.example.fitcnetdb.data.abcdefghi.local

import io.example.fitcnetdb.core.database.abcdefghi.AbcdefghiEntity
import io.example.fitcnetdb.data.abcdefghi.api.model.AbcdefghiItem
import io.example.fitcnetdb.data.abcdefghi.remote.dto.AbcdefghiDto
import io.example.fitcnetdb.data.abcdefghi.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghiDto.toEntity(position: Int): AbcdefghiEntity = toDomain().let { item ->
    AbcdefghiEntity(id = item.id, title = item.title, position = position)
}

internal fun AbcdefghiEntity.toDomain(): AbcdefghiItem = AbcdefghiItem(id = id, title = title)
