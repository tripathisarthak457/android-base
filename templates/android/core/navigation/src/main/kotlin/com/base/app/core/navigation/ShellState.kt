package com.base.app.core.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.navigation.back.StackChange
import com.base.app.core.navigation.back.Verdict
import com.base.app.core.navigation.back.guard
import com.base.app.core.navigation.back.submit
import com.base.app.core.navigation.shell.BarAnchor
import com.base.app.core.navigation.stack.ScreenBars

/**
 * One tab. [key] is the tab's root destination and doubles as its identity, so two tabs cannot
 * accidentally share a stack. Nothing keys composition on a [ShellTab] itself: an [ImageVector]
 * hashes by identity, so it would differ between processes and lose state restored after death.
 */
data class ShellTab(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

/**
 * The signed-in shell's navigation state: one back stack per tab, the tab in front, and the
 * presentation stack of sheets and covers shown above the tab bar.
 *
 * Every change goes through here, so the same rules hold whoever asks: a root key selects its
 * tab instead of being pushed, a presented key goes to the presentation stack, and a command that
 * arrives while its host is moving is dropped or waits (see [guard]).
 */
@Stable
class ShellState internal constructor(
    internal val rootKeys: List<AppNavKey>,
    internal val tabs: List<AppBackStack>,
    internal val presented: AppBackStack,
    initialTab: Int,
    /** Changes with every session: the shell keyed on it starts empty for the next person. */
    val generation: Int,
    private val isPresented: (AppNavKey) -> Boolean,
    private val sameGraph: (AppNavKey, AppNavKey) -> Boolean,
) {
    var selectedIndex by mutableIntStateOf(initialTab)
        private set

    /**
     * Whether the last switch was the user's (a tap, or Back at another tab's root), which the
     * incoming tab reveals; a deep link or a navigation to a root shows the tab at once.
     */
    internal var revealSwitch = false
        private set

    /** Counts reselections of each tab at its root, which scroll its primary list to the top. */
    internal val reselections: List<MutableIntState> = List(tabs.size) { mutableIntStateOf(0) }

    internal val anchors: List<BarAnchor> = List(tabs.size) { BarAnchor() }

    internal val screenBars: List<ScreenBars> = List(tabs.size) { ScreenBars() }

    internal val presentedBars = ScreenBars()

    /** The selected tab's stack. */
    val current: AppBackStack get() = tabs[selectedIndex]

    /** Where Back and an unaddressed command go: the presentation stack while anything is presented. */
    internal val front: AppBackStack get() = if (presented.size > 0) presented else current

    /** Whether the host in front is still moving, so a deep link waits before it is opened. */
    val moving: Boolean get() = presented.gate.moving || current.gate.moving

    val stacks: List<List<AppNavKey>> get() = tabs.map { it.keys }

    val presentedKeys: List<AppNavKey> get() = presented.keys

    val roots: List<AppNavKey> get() = rootKeys

    /**
     * A tap on a tab. Another tab is shown at once, as `UITabBarController` does. The selected tab
     * pops to its root in one animated pop, or, already there, asks its list to scroll to the top. A
     * tap while that tab is moving is ignored.
     */
    fun select(index: Int) {
        if (index !in tabs.indices) return
        if (index != selectedIndex) {
            revealSwitch = true
            selectedIndex = index
            return
        }
        val tab = tabs[index]
        if (tab.gate.moving) return
        if (tab.pop(tab.size) > 0) {
            tab.gate.changed(StackChange.Pop)
        } else {
            reselections[index].intValue++
        }
    }

    /** Applies [command] if its host allows it now; see [Verdict]. */
    internal fun submit(command: NavCommand): Verdict {
        if (command is NavCommand.Navigate && command.popUpTo == null) {
            val tab = rootKeys.indexOf(command.key)
            if (tab >= 0) return showRoot(tab)
        }
        val host = if (command is NavCommand.Navigate && isPresented(command.key)) presented else front
        val verdict = guard(command, host)
        if (verdict != Verdict.Apply) return verdict
        if (command is NavCommand.ResetTo && host !== presented) {
            resetTab(host, command.key)
        } else {
            host.apply(command)
            if (host !== presented) keepRoot(host)
        }
        host.gate.changed(host.change(command))
        return Verdict.Apply
    }

    /**
     * A deep link: a root key selects its tab; another key selects the tab whose root its feature
     * registered, then is pushed there, or presented above it.
     */
    internal fun open(key: AppNavKey) {
        val root = rootKeys.indexOf(key)
        if (root >= 0) {
            showRoot(root)
            return
        }
        revealSwitch = false
        selectedIndex = rootKeys.indexOfFirst { sameGraph(it, key) }.takeIf { it >= 0 } ?: selectedIndex
        val host = if (isPresented(key)) presented else current
        if (host.entries.lastOrNull() == key) return
        host.entries += key
        host.gate.changed(StackChange.Push)
    }

    /** Back at the root of another tab returns to the first one; at the first, Back leaves the app. */
    internal val canBackAtRoot: Boolean
        get() = presented.size == 0 && !current.canGoBack && !current.gate.moving && selectedIndex != 0

    internal fun backAtRoot(): Boolean {
        if (!canBackAtRoot) return false
        revealSwitch = true
        selectedIndex = 0
        return true
    }

    private fun showRoot(tab: Int): Verdict {
        val stack = tabs[tab]
        if (tab != selectedIndex) {
            stack.pop(stack.size)
            revealSwitch = false
            selectedIndex = tab
            return Verdict.Apply
        }
        if (stack.gate.moving) return Verdict.Wait
        if (stack.pop(stack.size) > 0) stack.gate.changed(StackChange.Pop)
        return Verdict.Apply
    }

    private fun resetTab(host: AppBackStack, key: AppNavKey) {
        val root = host.entries.first()
        host.entries.clear()
        host.entries += root
        if (key != root) host.entries += key
    }

    /** A command with popUpTo may empty a stack; a tab's root is never part of what it removes. */
    private fun keepRoot(host: AppBackStack) {
        val root = rootKeys[tabs.indexOf(host)]
        if (host.entries.firstOrNull() != root) host.entries.add(0, root)
    }
}

/**
 * The shell's state, surviving process death: every tab's stack, matched back to its tab by the
 * root key so reordering tabs between versions cannot restore a stack into the wrong one, the
 * selected tab and the presentation stack. A new [generation] starts empty.
 */
@Composable
fun rememberShellState(
    tabs: List<ShellTab>,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    generation: Int = 0,
): ShellState {
    val rootKeys = remember(tabs) { tabs.map { it.key } }
    val saver = remember(rootKeys, registry, serialization, generation) {
        shellSaver(rootKeys, registry, serialization, generation)
    }
    return rememberSaveable(rootKeys, generation, saver = saver) {
        newShellState(rootKeys, registry, generation, selected = 0, stacks = rootKeys.map(::listOf), presented = emptyList())
    }
}

internal fun newShellState(
    rootKeys: List<AppNavKey>,
    registry: NavRegistry,
    generation: Int,
    selected: Int,
    stacks: List<List<AppNavKey>>,
    presented: List<AppNavKey>,
): ShellState = ShellState(
    rootKeys = rootKeys,
    tabs = stacks.map { AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(it) }) },
    presented = AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(presented) }, floor = 0),
    initialTab = selected,
    generation = generation,
    isPresented = { registry.presentationOf(it) != null },
    sameGraph = registry::sameGraph,
)

/** Saved as `[selected root, presented, stack…]`, each one encoded stack. */
private fun shellSaver(
    rootKeys: List<AppNavKey>,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    generation: Int,
): Saver<ShellState, ArrayList<String>> = Saver(
    save = { state ->
        val parts = listOf(listOf(state.rootKeys[state.selectedIndex]), state.presentedKeys) + state.stacks
        parts.map { serialization.encodeStack(it) ?: return@Saver null }.toCollection(ArrayList())
    },
    restore = { saved ->
        val decoded = saved.map { serialization.decodeStack(it) }
        val savedStacks = decoded.drop(2).filterNotNull().filter { it.isNotEmpty() }
        val stacks = rootKeys.map { root -> savedStacks.firstOrNull { it.first() == root } ?: listOf(root) }
        val selected = decoded.firstOrNull()?.firstOrNull()?.let(rootKeys::indexOf)?.takeIf { it >= 0 } ?: 0
        newShellState(rootKeys, registry, generation, selected, stacks, decoded.getOrNull(1).orEmpty())
    },
)
