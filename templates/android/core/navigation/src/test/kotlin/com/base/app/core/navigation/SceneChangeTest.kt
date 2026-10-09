package com.base.app.core.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A fold, an unfold or a resize redraws the same stack in another scene; nothing should slide. */
class SceneChangeTest {

    private val motion = ScreenMotion()
    private val stack = AppBackStack(mutableStateListOf(FeedTab, Post(1)))
    private val single = KeepUnderSceneStrategy(motion, stack, anchor = null)
    private val twoPanes = ListDetailSceneStrategy(twoPanes = true, listPaneWidth = 320.dp, motion = motion, gate = stack.gate)

    @Test
    fun `unfolding a list and its detail into two panes is not navigation`() {
        val entries = listOf(entry(FeedTab, NavPane.List), entry(Post(1), NavPane.Detail))

        val phone = scene(single, entries)
        val tablet = scene(twoPanes, entries)

        assertTrue(sameStack(phone, tablet))
        assertTrue(sameStack(tablet, phone))
    }

    @Test
    fun `a push is navigation, whichever scene shows it`() {
        val root = listOf(entry(FeedTab, NavPane.List))
        val pushed = root + entry(Post(1), NavPane.Detail)

        assertFalse(sameStack(scene(single, root), scene(single, pushed)))
        assertFalse(sameStack(scene(single, pushed), scene(single, root)))
    }

    private fun entry(key: AppNavKey, pane: NavPane): NavEntry<AppNavKey> =
        NavEntry(key = key, metadata = mapOf(KEY_METADATA to key, PANE_METADATA_KEY to pane, HIDES_BAR_METADATA to false)) {}

    private fun scene(strategy: SceneStrategy<AppNavKey>, entries: List<NavEntry<AppNavKey>>): Scene<AppNavKey> =
        with(strategy) { SceneStrategyScope<AppNavKey>().calculateScene(entries) }
            ?: error("$strategy drew no scene for $entries")
}
