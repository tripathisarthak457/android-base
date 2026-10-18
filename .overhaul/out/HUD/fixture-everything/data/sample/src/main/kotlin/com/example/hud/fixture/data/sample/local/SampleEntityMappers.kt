package com.example.hud.fixture.data.sample.local

import com.example.hud.fixture.core.database.sample.SampleEntity
import com.example.hud.fixture.data.sample.api.model.SampleItem
import com.example.hud.fixture.data.sample.remote.dto.SampleDto
import com.example.hud.fixture.data.sample.remote.toDomain

/** [position] is the row's place in the server's list, which the table keeps for ordering. */
internal fun SampleDto.toEntity(position: Int): SampleEntity = toDomain().let { item ->
    SampleEntity(id = item.id, title = item.title, body = item.body, position = position)
}

internal fun SampleEntity.toDomain(): SampleItem = SampleItem(id = id, title = title, body = body)
