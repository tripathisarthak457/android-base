package io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.local

import io.example.fitcnetdb.core.database.abcdefghijklmnopqrstuvw.AbcdefghijklmnopqrstuvwEntity
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.api.model.AbcdefghijklmnopqrstuvwItem
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote.dto.AbcdefghijklmnopqrstuvwDto
import io.example.fitcnetdb.data.abcdefghijklmnopqrstuvw.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwDto.toEntity(position: Int): AbcdefghijklmnopqrstuvwEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuvwEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuvwEntity.toDomain(): AbcdefghijklmnopqrstuvwItem =
    AbcdefghijklmnopqrstuvwItem(id = id, title = title)
