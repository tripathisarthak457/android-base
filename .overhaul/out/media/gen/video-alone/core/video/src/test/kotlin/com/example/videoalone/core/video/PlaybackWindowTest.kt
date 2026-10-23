package com.example.videoalone.core.video

import com.example.videoalone.core.video.policy.PlaybackWindow
import com.example.videoalone.core.video.policy.PreloadTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWindowTest {

    @Test
    fun `the settled clip holds a player first, then the next in the direction of travel, then the one behind`() {
        assertEquals(listOf(5, 6, 4), PlaybackWindow.holders(current = 5, count = 12, poolSize = 3, forward = true))
        assertEquals(listOf(5, 4, 6), PlaybackWindow.holders(current = 5, count = 12, poolSize = 3, forward = false))
        assertEquals(listOf(5, 6), PlaybackWindow.holders(current = 5, count = 12, poolSize = 2, forward = true))
        assertEquals(listOf(5, 4), PlaybackWindow.holders(current = 5, count = 12, poolSize = 2, forward = false))
    }

    @Test
    fun `never more players than the pool, and none past the ends`() {
        assertEquals(listOf(0, 1, 2), PlaybackWindow.holders(current = 0, count = 12, poolSize = 3, forward = true))
        assertEquals(listOf(11, 10, 9), PlaybackWindow.holders(current = 11, count = 12, poolSize = 3, forward = true))
        assertEquals(listOf(0), PlaybackWindow.holders(current = 0, count = 1, poolSize = 3, forward = true))
        assertEquals(emptyList<Int>(), PlaybackWindow.holders(current = 3, count = 3, poolSize = 3, forward = true))
        for (current in 0 until 30) {
            assertTrue(PlaybackWindow.holders(current, count = 30, poolSize = 3, forward = current % 2 == 0).size <= 3)
        }
    }

    @Test
    fun `the preload window keeps neighbours in memory, the next ones briefly, the far ones on disk`() {
        assertEquals(PreloadTarget.Loaded(VideoDefaults.NEAR_PRELOAD_MS), PlaybackWindow.preload(1))
        assertEquals(PreloadTarget.Loaded(VideoDefaults.NEAR_PRELOAD_MS), PlaybackWindow.preload(-1))
        assertEquals(PreloadTarget.Loaded(VideoDefaults.FAR_PRELOAD_MS), PlaybackWindow.preload(3))
        assertEquals(PreloadTarget.Loaded(VideoDefaults.FAR_PRELOAD_MS), PlaybackWindow.preload(-3))
        assertEquals(PreloadTarget.Cached(VideoDefaults.CACHED_PRELOAD_MS), PlaybackWindow.preload(4))
        assertEquals(PreloadTarget.Cached(VideoDefaults.CACHED_PRELOAD_MS), PlaybackWindow.preload(6))
        assertNull(PlaybackWindow.preload(7))
        assertNull(PlaybackWindow.preload(-4))
    }

    @Test
    fun `only the settled clip plays, while shown, and under reduce motion only when asked`() {
        assertTrue(PlaybackWindow.plays(page = 2, current = 2, shown = true, reduceMotion = false, choice = null))
        assertFalse(PlaybackWindow.plays(page = 3, current = 2, shown = true, reduceMotion = false, choice = null))
        assertFalse(PlaybackWindow.plays(page = 2, current = 2, shown = false, reduceMotion = false, choice = true))
        assertFalse(PlaybackWindow.plays(page = 2, current = 2, shown = true, reduceMotion = true, choice = null))
        assertTrue(PlaybackWindow.plays(page = 2, current = 2, shown = true, reduceMotion = true, choice = true))
        assertFalse(PlaybackWindow.plays(page = 2, current = 2, shown = true, reduceMotion = false, choice = false))
    }
}
