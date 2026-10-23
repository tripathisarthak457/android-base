package com.example.videoalone.core.video

/**
 * The feed's engineering constants: not design tokens, because they shape memory and decoders, not
 * the look. Each is the value the phone measurements picked or a bound they stay within.
 */
internal object VideoDefaults {
    /**
     * Players, each holding a video decoder, a playback thread's share and buffers: the settled clip,
     * the next one and the previous one. A device runs a limited number of decoders at once.
     */
    const val POOL_SIZE = 3

    /** Pages the pager composes either side of the settled one, so a swipe reveals a frame, not a poster. */
    const val BEYOND_PAGES = 1

    /** Clips kept preloaded behind and ahead of the settled one; further ones are dropped to save memory. */
    const val LOOK_BEHIND = 3
    const val LOOK_AHEAD = 6

    /** How much of a neighbour, of the next two or three, and of the clips beyond them is fetched ahead. */
    const val NEAR_PRELOAD_MS = 2_000L
    const val FAR_PRELOAD_MS = 1_000L
    const val CACHED_PRELOAD_MS = 3_000L

    /**
     * Short buffers for short clips: playback starts after half a second and three players never hold
     * more than about 45 s of media, where Media3's defaults buffer 50 s each.
     */
    const val MIN_BUFFER_MS = 5_000
    const val MAX_BUFFER_MS = 15_000
    const val START_BUFFER_MS = 500
    const val REBUFFER_MS = 1_000

    /** The disk cache of video bytes, least recently used first out. */
    const val CACHE_BYTES = 128L * 1024 * 1024
}
