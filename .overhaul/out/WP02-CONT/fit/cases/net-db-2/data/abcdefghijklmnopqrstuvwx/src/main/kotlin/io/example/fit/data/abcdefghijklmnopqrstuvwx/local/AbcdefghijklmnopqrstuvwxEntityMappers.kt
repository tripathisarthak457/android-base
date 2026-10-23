package io.example.fit.data.abcdefghijklmnopqrstuvwx.local

import io.example.fit.core.database.abcdefghijklmnopqrstuvwx.AbcdefghijklmnopqrstuvwxEntity
import io.example.fit.data.abcdefghijklmnopqrstuvwx.api.model.AbcdefghijklmnopqrstuvwxItem
import io.example.fit.data.abcdefghijklmnopqrstuvwx.remote.dto.AbcdefghijklmnopqrstuvwxDto
import io.example.fit.data.abcdefghijklmnopqrstuvwx.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun AbcdefghijklmnopqrstuvwxDto.toEntity(position: Int): AbcdefghijklmnopqrstuvwxEntity =
    toDomain().let { item ->
        AbcdefghijklmnopqrstuvwxEntity(id = item.id, title = item.title, position = position)
    }

internal fun AbcdefghijklmnopqrstuvwxEntity.toDomain(): AbcdefghijklmnopqrstuvwxItem =
    AbcdefghijklmnopqrstuvwxItem(id = id, title = title)
