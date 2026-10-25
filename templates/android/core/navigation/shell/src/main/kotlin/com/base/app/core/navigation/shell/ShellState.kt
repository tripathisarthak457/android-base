package com.base.app.core.navigation.shell

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowMetrics
import com.base.app.core.designsystem.foundation.layout.ScreenChrome
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.NavCommand
import com.base.app.core.navigation.NavKeySerialization
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.TabStacks
import com.base.app.core.navigation.back.StackChange
import com.base.app.core.navigation.back.Verdict
import com.base.app.core.navigation.back.guard
import com.base.app.core.navigation.back.submit
import com.base.app.core.navigation.decodeStack
import com.base.app.core.navigation.encodeStack
import com.base.app.core.navigation.shell.stack.ScreenChromes
import com.base.app.core.navigation.shell.stack.lastContentKey

/**
 * One tab. [key] is the tab's root destination and doubles as its identity, so two tabs cannot
 * accidentally share a stack. Nothing keys composition on a [ShellTab] itself: an [ImageVector]
 * hashes by identity, so it would differ between processes and lose state restored after death.
 *
 * @param section groups tabs under a heading in the sidebar of large windows.
 */
data class ShellTab(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
    @param:StringRes val section: Int? = null,
)

/**
 * The signed-in shell's navigation state: the tabs' stacks and the tab in front ([TabStacks]), the
 * presentation stack of sheets and covers shown above the tab bar, and the chrome the window shows.
 * When the chrome has no room for every tab, the rest are listed under More, whose list can be in
 * front instead of a tab.
 *
 * Every change goes through here, so the same rules hold whoever asks: a root key selects its
 * tab instead of being pushed, a presented key goes to the presentation stack, and a command that
 * arrives while its host is moving is dropped or waits (see [guard]).
 */
@Stable
class ShellState internal constructor(
    /** Every tab's stack and the tab in front. */
    val tabStacks: TabStacks,
    internal val presented: AppBackStack,
    initialMore: Boolean = false,
    initialSidebar: Boolean? = null,
    /** Changes with every session: the shell keyed on it starts empty for the next person. */
    val generation: Int,
    private val isPresented: (AppNavKey) -> Boolean,
    private val sameGraph: (AppNavKey, AppNavKey) -> Boolean,
) {
    internal val rootKeys: List<AppNavKey> get() = tabStacks.roots

    internal val tabs: List<AppBackStack> get() = tabStacks.stacks

    /** The selected tab, by its place in the list of tabs; the More list may be in front of it ([moreShown]). */
    val selectedIndex: Int get() = tabStacks.selectedIndex

    /** Whether the More list is in front, in place of the tab at [selectedIndex]. */
    var moreShown by mutableStateOf(initialMore)
        private set

    /** The navigation chrome the window shows now: a bar, a rail or a sidebar. */
    var chrome by mutableStateOf(NavigationChrome.Bar)
        private set

    /** The size class of the window the shell is laid out for, which [chrome] follows. */
    var window: SizeClass? by mutableStateOf(null)
        private set

    /** The exact size of that window; read [WindowMetrics.sizeDp] in a lambda, never in composition. */
    var windowMetrics: WindowMetrics? by mutableStateOf(null)
        private set

    /** Whether the person opened (true) or folded (false) the sidebar; null until they do. */
    var sidebarOpen by mutableStateOf(initialSidebar)
        private set

    /**
     * The first tab the chrome has no room for: it and those after it are listed under More, at
     * this position in the chrome. The tab count when every tab fits.
     */
    var overflowFrom by mutableIntStateOf(tabs.size)
        private set

    /**
     * Whether the last switch was the user's (a tap, or Back at another tab's root), which the
     * incoming tab reveals; a deep link or a navigation to a root shows the tab at once.
     */
    internal var revealSwitch = false
        private set

    internal val anchors: List<BarAnchor> = List(tabs.size) { BarAnchor() }

    /** The More list never hides the bar. */
    private val moreAnchor = BarAnchor()

    /** What the bar follows: the screen in front of the selected tab, or the More list. */
    internal val barAnchor: BarAnchor get() = if (moreShown) moreAnchor else anchors[selectedIndex]

    internal val screens: List<ScreenChromes> = List(tabs.size) { ScreenChromes() }

    internal val presentedScreens = ScreenChromes()

    internal val moreScreen = ScreenChrome()

    /**
     * What the screen in front of the selected tab (or the More list) reports, which the bar and
     * the bottom edge fade are drawn for. Presented screens cover both, so they are not asked.
     */
    internal val tabFront: ScreenChrome?
        get() = if (moreShown) moreScreen else screens[selectedIndex][current.keys.lastContentKey()]

    /** The selected tab's stack. */
    val current: AppBackStack get() = tabStacks.current

    /** Where Back and an unaddressed command go: the presentation stack while anything is presented. */
    internal val front: AppBackStack get() = if (presented.size > 0) presented else current

    /** Whether the host in front is still moving, so a deep link waits before it is opened. */
    val moving: Boolean get() = presented.gate.moving || current.gate.moving

    /** A copy of every tab's stack, one list per tab in tab order, root first. */
    val stacks: List<List<AppNavKey>> get() = tabs.map { it.keys }

    /** A copy of the presentation stack, bottom first; empty when nothing is presented. */
    val presentedKeys: List<AppNavKey> get() = presented.keys

    /** Each tab's root destination, in tab order. */
    val roots: List<AppNavKey> get() = rootKeys

    /**
     * A tap on a tab. Another tab is shown at once, as `UITabBarController` does, and so is a tab the
     * More list is in front of. The selected tab pops to its root in one animated pop, or, already
     * there, asks its list to scroll to the top; a tap while that tab is moving is ignored
     * ([TabStacks.select]).
     */
    fun select(index: Int) {
        if (index !in tabs.indices) return
        if (moreShown) {
            revealSwitch = true
            moreShown = false
            tabStacks.show(index)
        } else if (tabStacks.select(index)) {
            revealSwitch = true
        }
    }

    /**
     * A tap on More: its list comes to the front, also from a tab it lists, as iOS's More tab pops
     * to its list. The tab keeps its stack.
     */
    fun showMore() {
        if (moreShown || overflowFrom >= tabs.size) return
        revealSwitch = true
        moreShown = true
    }

    /** Opens the sidebar on a window wide enough for one, or folds it to a rail. */
    fun showSidebar(open: Boolean) {
        sidebarOpen = open
    }

    /** The chrome [window] shows, and so where tabs start going under More. */
    internal fun useChrome(window: SizeClass, metrics: WindowMetrics, chrome: NavigationChrome) {
        this.window = window
        this.windowMetrics = metrics
        this.chrome = chrome
        overflowFrom = chrome.tabsShown(tabs.size)
        if (overflowFrom >= tabs.size) moreShown = false
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
        moreShown = false
        tabStacks.show(rootKeys.indexOfFirst { sameGraph(it, key) })
        val host = if (isPresented(key)) presented else current
        if (host.entries.lastOrNull() == key) return
        host.entries += key
        host.gate.changed(StackChange.Push)
    }

    /**
     * Back at the root of a tab listed under More returns to the More list; at the root of another
     * tab, or on the More list, to the first tab; at the first, Back leaves the app.
     */
    internal val canBackAtRoot: Boolean
        get() = presented.size == 0 &&
            (moreShown || (!current.canGoBack && !current.gate.moving && selectedIndex != 0))

    internal fun backAtRoot(): Boolean {
        if (!canBackAtRoot) return false
        revealSwitch = true
        if (!moreShown && selectedIndex >= overflowFrom) {
            moreShown = true
        } else {
            moreShown = false
            tabStacks.show(0)
        }
        return true
    }

    private fun showRoot(tab: Int): Verdict {
        val stack = tabs[tab]
        if (tab != selectedIndex || moreShown) {
            stack.pop(stack.size)
            revealSwitch = false
            moreShown = false
            tabStacks.show(tab)
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
        newShellState(TabStacks(rootKeys), registry, generation, more = false, sidebar = null, presented = emptyList())
    }
}

internal fun newShellState(
    tabStacks: TabStacks,
    registry: NavRegistry,
    generation: Int,
    more: Boolean,
    sidebar: Boolean?,
    presented: List<AppNavKey>,
): ShellState = ShellState(
    tabStacks = tabStacks,
    presented = AppBackStack(mutableStateListOf<AppNavKey>().apply { addAll(presented) }, floor = 0),
    initialMore = more,
    initialSidebar = sidebar,
    generation = generation,
    isPresented = { registry.presentationOf(it) != null },
    sameGraph = registry::sameGraph,
)

/**
 * Saved as `[flags, presented, tabs…]`: the flags say whether the More list is in front and what the
 * person last did with the sidebar, the presentation stack is one encoded stack, and the tabs are
 * what [TabStacks.saver] writes.
 */
private fun shellSaver(
    rootKeys: List<AppNavKey>,
    registry: NavRegistry,
    serialization: NavKeySerialization,
    generation: Int,
): Saver<ShellState, ArrayList<String>> {
    val tabsSaver = TabStacks.saver(rootKeys, serialization)
    return Saver(
        save = { state ->
            val presented = serialization.encodeStack(state.presentedKeys) ?: return@Saver null
            val tabs = with(tabsSaver) { save(state.tabStacks) } ?: return@Saver null
            val flags = listOfNotNull(
                MORE_SHOWN.takeIf { state.moreShown },
                state.sidebarOpen?.let { if (it) SIDEBAR_OPEN else SIDEBAR_FOLDED },
            )
            (listOf(flags.joinToString(","), presented) + tabs).toCollection(ArrayList())
        },
        restore = { saved ->
            val flags = saved.firstOrNull().orEmpty().split(",")
            val sidebar = when {
                SIDEBAR_OPEN in flags -> true
                SIDEBAR_FOLDED in flags -> false
                else -> null
            }
            val presented = saved.getOrNull(1)?.let(serialization::decodeStack).orEmpty()
            val tabs = tabsSaver.restore(ArrayList(saved.drop(2))) ?: TabStacks(rootKeys)
            newShellState(tabs, registry, generation, MORE_SHOWN in flags, sidebar, presented)
        },
    )
}

private const val MORE_SHOWN = "more"
private const val SIDEBAR_OPEN = "sidebar"
private const val SIDEBAR_FOLDED = "rail"
