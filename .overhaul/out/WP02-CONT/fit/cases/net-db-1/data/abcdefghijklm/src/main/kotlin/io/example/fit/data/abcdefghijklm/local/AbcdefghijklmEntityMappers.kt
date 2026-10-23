package io.example.fit.data.abcdefghijklm.local

import io.example.fit.core.database.abcdefghijklm.AbcdefghijklmEntity
import io.example.fit.data.abcdefghijklm.api.model.AbcdefghijklmItem
import io.example.fit.data.abcdefghijklm.remote.dto.AbcdefghijklmDto
import io.example.fit.data.abcdefghijklm.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmDto.toEntity(position: Int): AbcdefghijklmEntity =
    toDomain().let { item ->
        AbcdefghijklmEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmEntity.toDomain(): AbcdefghijklmItem =
    AbcdefghijklmItem(id = id, title = title)
