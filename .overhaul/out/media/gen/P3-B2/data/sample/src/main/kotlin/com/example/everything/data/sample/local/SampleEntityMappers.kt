package com.example.everything.data.sample.local

import com.example.everything.core.database.sample.SampleEntity
import com.example.everything.data.sample.api.model.SampleItem
import com.example.everything.data.sample.remote.dto.SampleDto
import com.example.everything.data.sample.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun SampleDto.toEntity(position: Int): SampleEntity = toDomain().let { item ->
    SampleEntity(id = item.id, title = item.title, body = item.body, position = position)
}

internal fun SampleEntity.toDomain(): SampleItem = SampleItem(id = id, title = title, body = body)
