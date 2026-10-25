package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.back.StackChange

/**
 * One back stack per tab and the tab in front, as in the Navigation 3 recipe for multiple back
 * stacks: a tab keeps its history while another is shown, and comes back as it was left. It draws
 * nothing; a host shows [current] and its bar calls [select]. [rememberTabStacks] keeps it through
 * process death.
 *
 * A tap on the tab already in front goes back to that tab's root in one change, or, at the root,
 * counts a reselection, which [ScrollToTopOnReselect] turns into a scroll to the top.
 */
@Stable
class TabStacks internal constructor(
    /** Each tab's root destination, in tab order. A root is also its tab's identity. */
    val roots: List<AppNavKey>,
    stacks: List<List<AppNavKey>>,
    selected: Int,
) {
    /** Every tab at its root, the first in front. */
    internal constructor(roots: List<AppNavKey>) : this(roots, roots.map(::listOf), selected = 0)

    init {
        require(roots.isNotEmpty()) { "TabStacks needs at least one tab." }
        require(stacks.size == roots.size && stacks.indices.all { stacks[it].firstOrNull() == roots[it] }) {
            "Each tab's stack starts at that tab's root."
        }
    }

    /** Each tab's back stack, in tab order, root first. */
    val stacks: List<AppBackStack> = stacks.map { AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(it) }) }

    /** The tab in front, by its place in [roots]. */
    var selectedIndex by mutableIntStateOf(selected.takeIf { it in roots.indices } ?: 0)
        private set

    /** The stack of the tab in front. */
    val current: AppBackStack get() = stacks[selectedIndex]

    private val reselected = List(roots.size) { mutableIntStateOf(0) }

    /** How many times tab [index] was tapped while in front at its root. */
    fun reselections(index: Int): IntState = reselected[index]

    /**
     * A tap on tab [index]. Another tab comes to the front at once, its stack as it was left. The tab
     * in front goes back to its root in one change, or, at the root, counts a reselection; a tap on
     * it while its screens move does nothing.
     *
     * @return whether another tab came to the front.
     */
    fun select(index: Int): Boolean {
        if (index !in roots.indices) return false
        if (index != selectedIndex) {
            selectedIndex = index
            return true
        }
        val tab = stacks[index]
        if (tab.gate.moving) return false
        if (tab.pop(tab.size) > 0) {
            tab.gate.changed(StackChange.Pop)
        } else {
            reselected[index].intValue++
        }
        return false
    }

    /** Brings tab [index] to the front as it was left, with none of [select]'s reselection. */
    fun show(index: Int) {
        if (index in roots.indices) selectedIndex = index
    }

    companion object {
        /**
         * Saves every stack and the tab in front as `[tab in front's root, stack…]`. A stack goes back
         * to the tab whose root it starts with, so tabs reordered between two runs each get their
         * own; a tab with no saved stack starts at its root.
         */
        fun saver(roots: List<AppNavKey>, serialization: NavKeySerialization): Saver<TabStacks, ArrayList<String>> =
            Saver(
                save = { tabs ->
                    val parts = listOf(listOf(tabs.roots[tabs.selectedIndex])) + tabs.stacks.map { it.keys }
                    parts.mapTo(ArrayList()) { serialization.encodeStack(it) ?: return@Saver null }
                },
                restore = { saved ->
                    val decoded = saved.map { serialization.decodeStack(it) }
                    val stacks = decoded.drop(1).filterNotNull().filter { it.isNotEmpty() }
                    val selected = decoded.firstOrNull()?.firstOrNull()?.let(roots::indexOf) ?: 0
                    TabStacks(
                        roots = roots,
                        stacks = roots.map { root -> stacks.firstOrNull { it.first() == root } ?: listOf(root) },
                        selected = selected,
                    )
                },
            )
    }
}

/**
 * Tabs with a back stack each, starting at [roots], kept across configuration changes and process
 * death. Every key on a stack is one a nav graph registers, so [serialization] can write it.
 */
@Composable
fun rememberTabStacks(roots: List<AppNavKey>, serialization: NavKeySerialization): TabStacks {
    val saver = remember(roots, serialization) { TabStacks.saver(roots, serialization) }
    return rememberSaveable(roots, saver = saver) {
        TabStacks(roots)
    }
}
