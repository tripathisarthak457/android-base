package com.example.finalafter.crui.core.perfhud.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PssScheduleTest {

    private val schedule = PssSchedule(intervalMillis = 15_000L)

    @Test
    fun `the first read waits only for a pause in the drawing`() {
        assertFalse(schedule.shouldRead(nowMillis = 0L, drawing = true))
        assertFalse(schedule.shouldRead(nowMillis = 1_000L, drawing = true))
        assertTrue(schedule.shouldRead(nowMillis = 2_000L, drawing = false))
    }

    @Test
    fun `reads are at least the interval apart`() {
        assertTrue(schedule.shouldRead(nowMillis = 0L, drawing = false))
        assertFalse(schedule.shouldRead(nowMillis = 14_999L, drawing = false))
        assertTrue(schedule.shouldRead(nowMillis = 15_000L, drawing = false))
    }

    @Test
    fun `a read that is due waits while the app draws`() {
        assertTrue(schedule.shouldRead(nowMillis = 0L, drawing = false))
        assertFalse(schedule.shouldRead(nowMillis = 16_000L, drawing = true))
        assertFalse(schedule.shouldRead(nowMillis = 40_000L, drawing = true))
        assertTrue(schedule.shouldRead(nowMillis = 41_000L, drawing = false))
        assertFalse(schedule.shouldRead(nowMillis = 42_000L, drawing = false))
    }
}
