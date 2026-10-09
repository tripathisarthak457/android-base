package com.base.app.data.feed.api

data class FeedPost(
    val id: Int,
    val authorId: Int,
    val title: String,
    val body: String,
)
