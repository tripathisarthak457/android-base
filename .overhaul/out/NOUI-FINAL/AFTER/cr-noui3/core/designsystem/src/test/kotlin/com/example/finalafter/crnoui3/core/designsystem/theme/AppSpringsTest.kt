package com.example.finalafter.crnoui3.core.designsystem.theme

import kotlin.math.PI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSpringsTest {

    @Test
    fun `a spring's numbers follow iOS's response and bounce`() {
        val spring = AppSpring(response = 0.24f, bounce = 0f)
        assertEquals(((2 * PI / 0.24) * (2 * PI / 0.24)).toFloat(), spring.stiffness, 0.5f)
        assertEquals(1f, spring.dampingRatio, 0f)
        assertEquals(0.85f, AppSpring(response = 0.3f, bounce = 0.15f).dampingRatio, 0.0001f)
    }

    @Test
    fun `full-screen moves never bounce`() {
        val springs = DefaultSprings
        listOf(springs.screen, springs.sheet, springs.dialog, springs.layout, springs.thumb, springs.press).forEach {
            assertEquals(0f, it.bounce, 0f)
        }
    }

    @Test
    fun `content brought back from past an edge never overshoots it`() {
        assertTrue(DefaultSprings.bounceBack.dampingRatio > 1f)
    }
}
