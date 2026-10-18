package com.example.hud.standard.core.perfhud.log

import com.example.hud.standard.core.perfhud.frame.FrameLog
import com.example.hud.standard.core.perfhud.memory.GcCounters
import com.example.hud.standard.core.perfhud.memory.MemoryLog
import com.example.hud.standard.core.perfhud.memory.MemoryReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfLogcatTest {

    private val lines = mutableListOf<String>()
    private val logcat = PerfLogcat { lines += it }
    private val frames = FrameLog(capacity = 256)
    private val memory = MemoryLog()
    private var clock = 0L

    @Test
    fun `a window line every five seconds of drawing, none per frame`() {
        logcat.begin(frames)
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(1, 2, 0, 0)))
        draw(frames = 600, screen = "Home")

        repeat(4) { logcat.onSecond(frames, memory, screen = "Home") }
        assertTrue(lines.isEmpty())
        logcat.onSecond(frames, memory, screen = "Home")

        assertEquals(1, lines.size)
        val line = lines.single()
        assertTrue(line, line.startsWith("window=5s frames=600 fps=120.0 janky=0.0% "))
        assertTrue(line, line.contains(" heapMb=20.0 nativeMb=10.0 gc=0 screen=Home"))
    }

    @Test
    fun `a window with nothing drawn writes nothing`() {
        draw(frames = 10, screen = "Home")
        logcat.begin(frames)

        repeat(10) { logcat.onSecond(frames, memory, screen = "Home") }

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `leaving a screen writes its summary`() {
        logcat.begin(frames)
        draw(frames = 99, screen = "Home")
        draw(frames = 1, screen = "Home", missedDeadline = true)
        logcat.onSecond(frames, memory, screen = "Home")

        logcat.onSecond(frames, memory, screen = "Detail")

        assertEquals(1, lines.size)
        assertTrue(lines.single(), lines.single().startsWith("screen=Home frames=100 janky=1.0% p50="))
    }

    @Test
    fun `frames from before recording started are not counted again`() {
        draw(frames = 500, screen = "Home", missedDeadline = true)
        logcat.begin(frames)
        draw(frames = 100, screen = "Home")

        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }

        assertTrue(lines.single(), lines.single().startsWith("window=5s frames=100 fps=120.0 janky=0.0% "))
    }

    private fun draw(frames: Int, screen: String, missedDeadline: Boolean = false) {
        repeat(frames) {
            clock += INTERVAL
            this.frames.add(clock, 3 * MILLI, 6 * MILLI, INTERVAL, missedDeadline, screen)
        }
    }

    private companion object {
        const val MILLI = 1_000_000L
        const val INTERVAL = 8_333_333L
    }
}
