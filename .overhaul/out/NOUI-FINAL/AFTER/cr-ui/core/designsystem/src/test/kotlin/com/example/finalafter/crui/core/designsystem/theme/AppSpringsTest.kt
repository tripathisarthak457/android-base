package com.example.finalafter.crui.core.designsystem.theme

import kotlin.math.PI
import org.junit.Assert.assertEquals
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
    fun `full-screen moves never bounce, in any style`() {
        AppMotionStyle.entries.forEach { style ->
            val springs = style.springs()
            listOf(springs.screen, springs.sheet, springs.dialog, springs.layout, springs.thumb, springs.press).forEach {
                assertEquals("$style", 0f, it.bounce, 0f)
            }
        }
    }

    @Test
    fun `calm overshoots nowhere and travels slower than standard`() {
        val calm = AppMotionStyle.Calm.springs()
        listOf(calm.release, calm.thumb, calm.indicatorLead, calm.emphasis, calm.interactive, calm.iconBounceUp)
            .forEach { assertEquals(0f, it.bounce, 0f) }
        assertEquals(true, calm.bounceBack.dampingRatio > 1f)
        assertEquals(true, calm.screen.response > AppMotionStyle.Standard.springs().screen.response)
    }

    @Test
    fun `each style's springs are built once`() {
        AppMotionStyle.entries.forEach { assertEquals(true, it.springs() === it.springs()) }
    }
}
