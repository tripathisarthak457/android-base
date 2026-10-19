package com.example.wp04.everything.core.navigation.stack

import com.example.wp04.everything.core.navigation.NavPane
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StackPlanTest {

    @Test
    fun `a list and the details over it share a page when there is room for two`() {
        val panes = listOf(NavPane.Single, NavPane.List, NavPane.Detail, NavPane.Detail, NavPane.Detail)

        assertEquals(listOf(0..0, 1..4), stackPages(panes, columns = 2))
        assertEquals(listOf(0..0, 1..1, 2..2, 3..3, 4..4), stackPages(panes, columns = 1))
    }

    @Test
    fun `a detail with no list under it is a page of its own`() {
        assertEquals(listOf(0..0, 1..1), stackPages(listOf(NavPane.Single, NavPane.Detail), columns = 2))
    }

    @Test
    fun `a two-pane page shows its list and newest detail, or an empty detail pane`() {
        val panes = listOf(NavPane.List, NavPane.Detail, NavPane.Detail)
        val pages = pagesOf(listOf("list", "a", "b"), panes, columns = 2)
        assertEquals(mapOf("list" to Slot.List, "b" to Slot.Detail), pages.single().shown)

        val empty = pagesOf(listOf("list"), listOf(NavPane.List), columns = 2)
        assertEquals(mapOf("list" to Slot.List, EmptyDetail("list") to Slot.Detail), empty.single().shown)
    }

    @Test
    fun `with one column a list fills its page, with no empty detail beside it`() {
        val root = pagesOf(listOf("list"), listOf(NavPane.List), columns = 1)
        assertEquals(mapOf("list" to Slot.Full), root.single().shown)

        val pushed = pagesOf(listOf("list", "a"), listOf(NavPane.List, NavPane.Detail), columns = 1)
        assertEquals(listOf(mapOf<Any, Slot>("list" to Slot.Full), mapOf<Any, Slot>("a" to Slot.Full)), pushed.map { it.shown })
        assertTrue(planChange(null, listOf("list"), listOf(NavPane.List), columns = 1) { true }.emptyDetails.isEmpty())
    }

    @Test
    fun `an extra joins the page of the detail it was opened from`() {
        val panes = listOf(NavPane.List, NavPane.Detail, NavPane.Extra)
        val keys = listOf("list", "a", "history")

        assertEquals(listOf(0..2), stackPages(panes, columns = 2))
        assertEquals(
            mapOf("list" to Slot.List, "a" to Slot.Detail, "history" to Slot.Extra),
            pagesOf(keys, panes, columns = 3).single().shown,
        )
        assertEquals(
            mapOf("a" to Slot.Detail, "history" to Slot.Extra),
            pagesOf(keys, panes, columns = 2).single().shown,
        )
        assertEquals(listOf(0..0, 1..1), stackPages(listOf(NavPane.List, NavPane.Extra), columns = 3))
    }

    @Test
    fun `a supporting pane sits beside its main screen, and over it on a phone`() {
        val panes = listOf(NavPane.Main, NavPane.Supporting)

        assertEquals(
            mapOf("map" to Slot.Main, "place" to Slot.Supporting),
            pagesOf(listOf("map", "place"), panes, columns = 2).single().shown,
        )
        assertEquals(listOf(0..0, 1..1), stackPages(panes, columns = 1))
    }

    @Test
    fun `panes split the width, the detail taking what the side panes leave`() {
        val panes = listOf(NavPane.List, NavPane.Detail, NavPane.Extra)
        val plan = planChange(null, listOf("list", "a", "history"), panes, columns = 3) { true }

        assertEquals(PaneBounds(0, 400), plan.boundsOf("list", width = 1600, pane = 400))
        assertEquals(PaneBounds(400, 800), plan.boundsOf("a", width = 1600, pane = 400))
        assertEquals(PaneBounds(1200, 400), plan.boundsOf("history", width = 1600, pane = 400))
        assertTrue(plan.followsAnotherPane("a"))
        assertFalse(plan.followsAnotherPane("list"))
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
        val phone = planChange(null, listOf("list", "post"), panes, columns = 1) { true }
        val tablet = planChange(phone, listOf("list", "post"), panes, columns = 2) { true }

        assertEquals(Slide.None, tablet.slide)
        assertEquals(Slide.None, planChange(tablet, listOf("list", "post"), panes, columns = 1) { true }.slide)
    }

    @Test
    fun `another detail beside the same list swaps in place`() {
        val panes = listOf(NavPane.List, NavPane.Detail)
        val first = planChange(null, listOf("list", "a"), panes, columns = 2) { true }
        val second = planChange(first, listOf("list", "b"), panes, columns = 2) { true }

        assertEquals(Slide.None, second.slide)
    }

    @Test
    fun `a destination that asks for no transition does not slide`() {
        val plan = change(listOf("root"), listOf("root", "still")) { it != "still" }

        assertEquals(Slide.None, plan.slide)
        assertEquals(setOf("still"), plan.upper.keys)
    }

    private fun change(from: List<Any>, to: List<Any>, slides: (Any) -> Boolean = { true }): StackPlan {
        val before = planChange(null, from, from.map { NavPane.Single }, columns = 1, slides)
        return planChange(before, to, to.map { NavPane.Single }, columns = 1, slides)
    }
}
