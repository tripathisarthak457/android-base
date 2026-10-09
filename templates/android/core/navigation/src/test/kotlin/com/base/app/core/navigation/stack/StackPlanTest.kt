package com.base.app.core.navigation.stack

import com.base.app.core.navigation.NavPane
import org.junit.Assert.assertEquals
import org.junit.Test

class StackPlanTest {

    @Test
    fun `a list and the details over it share a page when there is room for two`() {
        val panes = listOf(NavPane.Single, NavPane.List, NavPane.Detail, NavPane.Detail, NavPane.Detail)

        assertEquals(listOf(0..0, 1..4), stackPages(panes, twoPanes = true))
        assertEquals(listOf(0..0, 1..1, 2..2, 3..3, 4..4), stackPages(panes, twoPanes = false))
    }

    @Test
    fun `a detail with no list under it is a page of its own`() {
        assertEquals(listOf(0..0, 1..1), stackPages(listOf(NavPane.Single, NavPane.Detail), twoPanes = true))
    }

    @Test
    fun `a two-pane page shows its list and newest detail, or an empty detail pane`() {
        val panes = listOf(NavPane.List, NavPane.Detail, NavPane.Detail)
        val pages = pagesOf(listOf("list", "a", "b"), panes, twoPanes = true)
        assertEquals(mapOf("list" to Slot.List, "b" to Slot.Detail), pages.single().shown)

        val empty = pagesOf(listOf("list"), listOf(NavPane.List), twoPanes = true)
        assertEquals(mapOf("list" to Slot.List, EmptyDetail("list") to Slot.Detail), empty.single().shown)
    }

    @Test
    fun `a new screen on top slides in over the one it covers`() {
        val plan = change(listOf("root"), listOf("root", "post"))

        assertEquals(Slide.Push, plan.slide)
        assertEquals(setOf("post"), plan.upper.keys)
        assertEquals(setOf("root"), plan.lower.keys)
    }

    @Test
    fun `a pop slides the old top off the page it returns to, however many it pops`() {
        val plan = change(listOf("root", "a", "b", "c"), listOf("root"))

        assertEquals(Slide.Pop, plan.slide)
        assertEquals(setOf("c"), plan.upper.keys)
        assertEquals(setOf("root"), plan.lower.keys)
    }

    @Test
    fun `a replace slides in like a push`() {
        assertEquals(Slide.Push, change(listOf("root", "signin"), listOf("root", "home")).slide)
    }

    @Test
    fun `an unfold that puts a list and its detail side by side is not navigation`() {
        val panes = listOf(NavPane.List, NavPane.Detail)
        val phone = planChange(null, listOf("list", "post"), panes, twoPanes = false) { true }
        val tablet = planChange(phone, listOf("list", "post"), panes, twoPanes = true) { true }

        assertEquals(Slide.None, tablet.slide)
        assertEquals(Slide.None, planChange(tablet, listOf("list", "post"), panes, twoPanes = false) { true }.slide)
    }

    @Test
    fun `another detail beside the same list swaps in place`() {
        val panes = listOf(NavPane.List, NavPane.Detail)
        val first = planChange(null, listOf("list", "a"), panes, twoPanes = true) { true }
        val second = planChange(first, listOf("list", "b"), panes, twoPanes = true) { true }

        assertEquals(Slide.None, second.slide)
    }

    @Test
    fun `a destination that asks for no transition does not slide`() {
        val plan = change(listOf("root"), listOf("root", "still")) { it != "still" }

        assertEquals(Slide.None, plan.slide)
        assertEquals(setOf("still"), plan.upper.keys)
    }

    private fun change(from: List<Any>, to: List<Any>, slides: (Any) -> Boolean = { true }): StackPlan {
        val before = planChange(null, from, from.map { NavPane.Single }, twoPanes = false, slides)
        return planChange(before, to, to.map { NavPane.Single }, twoPanes = false, slides)
    }
}
