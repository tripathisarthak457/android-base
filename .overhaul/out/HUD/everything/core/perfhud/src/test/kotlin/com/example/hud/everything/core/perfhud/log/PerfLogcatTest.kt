package com.example.hud.everything.core.perfhud.log

import com.example.hud.everything.core.perfhud.frame.FrameLog
import com.example.hud.everything.core.perfhud.memory.GcCounters
import com.example.hud.everything.core.perfhud.memory.MemoryLog
import com.example.hud.everything.core.perfhud.memory.MemoryReading
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
        logcat.begin(frames, memory)
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
    fun `each window line counts the collections of its own window`() {
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(1, 2, 0, 0)))
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(4, 9, 0, 0)))
        logcat.begin(frames, memory)
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(6, 12, 0, 0)))
        draw(frames = 600, screen = "Home")
        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(7, 13, 0, 0)))
        draw(frames = 600, screen = "Home")

        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }

        assertEquals(2, lines.size)
        assertTrue(lines[0], lines[0].contains(" gc=2 "))
        assertTrue(lines[1], lines[1].contains(" gc=1 "))
    }

    @Test
    fun `a window line does not count the collections of a quiet window before it`() {
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(1, 2, 0, 0)))
        logcat.begin(frames, memory)
        draw(frames = 600, screen = "Home")
        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(5, 20, 0, 0)))
        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }
        memory.add(MemoryReading(20f, 10f, Float.NaN, Float.NaN, GcCounters(6, 22, 0, 0)))
        draw(frames = 600, screen = "Home")

        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }

        assertEquals(2, lines.size)
        assertTrue(lines[1], lines[1].contains(" gc=1 "))
    }

    @Test
    fun `a window line gives full-duration and main-thread percentiles apart`() {
        logcat.begin(frames, memory)
        draw(frames = 600, screen = "Home")

        repeat(5) { logcat.onSecond(frames, memory, screen = "Home") }

        val line = lines.single()
        assertTrue(line, line.contains(" frameP50=6.1 "))
        assertTrue(line, line.contains(" uiP50=3.1 "))
    }

    @Test
    fun `a window with nothing drawn writes nothing`() {
        draw(frames = 10, screen = "Home")
        logcat.begin(frames, memory)

        repeat(10) { logcat.onSecond(frames, memory, screen = "Home") }

        assertTrue(lines.isEmpty())
    }

    @Test
    fun `leaving a screen writes its summary`() {
        logcat.begin(frames, memory)
        draw(frames = 99, screen = "Home")
        draw(frames = 1, screen = "Home", missedDeadline = true)
        logcat.onSecond(frames, memory, screen = "Home")

        logcat.onSecond(frames, memory, screen = "Detail")

        assertEquals(1, lines.size)
        assertTrue(lines.single(), lines.single().startsWith("screen=Home frames=100 janky=1.0% frameP50="))
    }

    @Test
    fun `frames from before recording started are not counted again`() {
        draw(frames = 500, screen = "Home", missedDeadline = true)
        logcat.begin(frames, memory)
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
