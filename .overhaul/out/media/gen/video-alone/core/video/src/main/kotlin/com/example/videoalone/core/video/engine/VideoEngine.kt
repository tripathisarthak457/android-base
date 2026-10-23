package com.example.videoalone.core.video.engine

import androidx.media3.common.Player
import com.example.videoalone.core.video.VideoClip
import com.example.videoalone.core.video.policy.PreloadTarget

/**
 * What a feed plays with: a small pool of players and the preloading behind them. The feed's state
 * drives it on the main thread; tests replace it with a fake.
 */
internal interface VideoEngine {
    /** An idle player from the pool, built the first time it is needed; the feed never asks for more than the pool. */
    fun obtain(): Player

    /** Points [player] at [clip] as page [page], starting at [positionMs], from its preloaded source if it has one. */
    fun prepare(player: Player, page: Int, clip: VideoClip, positionMs: Long)

    /** Takes [player] off its clip and back into the pool. */
    fun recycle(player: Player)

    /** The settled page, which the preload ranks the others by. */
    fun settle(page: Int)

    /** The clips to keep ready, by page; pages missing from [targets] are dropped. */
    fun preload(clips: List<VideoClip>, targets: Map<Int, PreloadTarget>)

    /** Releases the preloading, then every player: the feed's screen has stopped. */
    fun release()
}
