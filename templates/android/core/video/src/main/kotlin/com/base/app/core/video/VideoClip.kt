package com.base.app.core.video

import androidx.compose.runtime.Immutable

/**
 * One clip of a feed: where it plays from, the picture shown until its first frame, and its size in
 * pixels, so the page can be laid out before anything has loaded. [id] keys the clip's disk cache, so
 * a clip that appears twice in a feed is fetched once.
 */
@Immutable
data class VideoClip(
    val id: String,
    val url: String,
    val posterUrl: String?,
    val width: Int,
    val height: Int,
)
