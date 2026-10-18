package com.example.hud.fixture.data.feed.api.model

/** One post in the feed. */
data class FeedPost(
    val id: Int,
    val authorId: Int,
    val title: String,
    /** The text as the author wrote it, line breaks included, for the expanded post. */
    val body: String,
    /** [body] on one line, for a card that shows two lines of it without ragged breaks. */
    val excerpt: String,
)
