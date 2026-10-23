package io.example.fit.data.abcdefghijklmnopqrstuvwxy.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwxy.AbcdefghijklmnopqrstuvwxyEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.api.model.AbcdefghijklmnopqrstuvwxyItem
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.dto.AbcdefghijklmnopqrstuvwxyDto
import io.example.fit.data.abcdefghijklmnopqrstuvwxy.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxyDto.toEntity(position: Int): AbcdefghijklmnopqrstuvwxyEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuvwxyEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuvwxyEntity.toDomain(): AbcdefghijklmnopqrstuvwxyItem =
    AbcdefghijklmnopqrstuvwxyItem(id = id, title = title)
