package com.example.wp04.everything.data.feed.remote

import com.example.wp04.everything.data.feed.api.FeedPost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedPostDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("userId") val userId: Int = 0,
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
)

internal fun FeedPostDto.toDomain(): FeedPost = FeedPost(
    id = id,
    authorId = userId,
    title = title.trim().replaceFirstChar { it.uppercase() },
    body = body.trim(),
)
