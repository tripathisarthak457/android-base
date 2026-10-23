package io.example.fit.data.abcdefghijklmn.local

import io.example.fit.core.database.abcdefghijklmn.AbcdefghijklmnEntity
import io.example.fit.data.abcdefghijklmn.api.model.AbcdefghijklmnItem
import io.example.fit.data.abcdefghijklmn.remote.dto.AbcdefghijklmnDto
import io.example.fit.data.abcdefghijklmn.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnDto.toEntity(position: Int): AbcdefghijklmnEntity =
    toDomain().let { item ->
        AbcdefghijklmnEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnEntity.toDomain(): AbcdefghijklmnItem =
    AbcdefghijklmnItem(id = id, title = title)
