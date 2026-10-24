package io.example.audit.data.sample.local

import io.example.audit.core.database.sample.SampleEntity
import io.example.audit.data.sample.api.model.SampleItem
import io.example.audit.data.sample.remote.dto.SampleDto
import io.example.audit.data.sample.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun SampleDto.toEntity(position: Int): SampleEntity = toDomain().let { item ->
    SampleEntity(id = item.id, title = item.title, body = item.body, position = position)
}

internal fun SampleEntity.toDomain(): SampleItem = SampleItem(id = id, title = title, body = body)
