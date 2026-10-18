package com.example.hud.everything.data.feed.remote

import com.example.hud.everything.data.feed.api.model.FeedPost
import com.example.hud.everything.data.feed.remote.dto.FeedPostDto

internal fun FeedPostDto.toDomain(): FeedPost = FeedPost(
    id = id,
    authorId = userId,
    title = title.trim().replaceFirstChar { it.uppercase() },
    body = body.trim(),
    excerpt = body.trim().replace(WHITESPACE, " "),
)

private val WHITESPACE = Regex("\\s+")
