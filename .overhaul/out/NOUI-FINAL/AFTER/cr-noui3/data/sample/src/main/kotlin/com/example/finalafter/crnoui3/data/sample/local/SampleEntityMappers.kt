package com.example.finalafter.crnoui3.data.sample.local

import com.example.finalafter.crnoui3.core.database.sample.SampleEntity
import com.example.finalafter.crnoui3.data.sample.api.model.SampleItem
import com.example.finalafter.crnoui3.data.sample.remote.dto.SampleDto
import com.example.finalafter.crnoui3.data.sample.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun SampleDto.toEntity(position: Int): SampleEntity = toDomain().let { item ->
    SampleEntity(id = item.id, title = item.title, body = item.body, position = position)
}

internal fun SampleEntity.toDomain(): SampleItem = SampleItem(id = id, title = title, body = body)
