package io.example.fit.data.abcdefghijklmno.local

import io.example.fit.core.database.abcdefghijklmno.AbcdefghijklmnoEntity
import io.example.fit.data.abcdefghijklmno.api.model.AbcdefghijklmnoItem
import io.example.fit.data.abcdefghijklmno.remote.dto.AbcdefghijklmnoDto
import io.example.fit.data.abcdefghijklmno.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnoDto.toEntity(position: Int): AbcdefghijklmnoEntity =
    toDomain().let { item ->
        AbcdefghijklmnoEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnoEntity.toDomain(): AbcdefghijklmnoItem =
    AbcdefghijklmnoItem(id = id, title = title)
