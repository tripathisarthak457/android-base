package com.base.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.base.app.core.designsystem.component.navigation.AppBottomBar
import com.base.app.core.designsystem.component.navigation.BottomNavItem
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * One tab.
 *
 * [key] is the tab's root destination and doubles as its identity, so two tabs cannot
 * accidentally share a stack.
 *
 * [badgeCount] is a plain value the caller recomputes — a cart count, unread notifications. It is
 * not a flow, because the shell is composed inside whatever already collects that state, and a
 * second collector per tab would be four collectors for one number.
 */
data class ShellTab(
    val key: AppNavKey,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

/**
 * The tab bar's state: one back stack per tab, and which one is in front.
 *
 * ## Why per-tab stacks
 *
 * The simpler design — one stack, and switching tabs resets it — loses the user's place. Someone
 * three screens deep in Orders who checks Profile and comes back expects to be where they left
 * off, and every app they use behaves that way. The cost is this class; the alternative costs a
 * complaint in every review cycle.
 *
 * ## Why not one stack with markers
 *
 * A single list with tab boundaries in it has to answer "what does Back do at a boundary" for
 * every position in the list, and gets it subtly wrong somewhere. Separate lists make the answer
 * structural: back inside a tab pops that tab, back at a tab root is the shell's decision.
 */
class ShellState internal constructor(
    internal val stacks: List<SnapshotStateList<AppNavKey>>,
    initialTab: Int,
) {
    var selectedIndex by mutableIntStateOf(initialTab)
        internal set

    /** The stack that is currently on screen. Every navigation command applies to this one. */
    val current: AppBackStack get() = AppBackStack(stacks[selectedIndex])

    /**
     * Switches tabs, or — when the tab is already selected — returns it to its root.
     *
     * Re-tapping the active tab to get back to the top is a gesture people use constantly and
     * almost never discover being told about; a tab that ignores its own re-tap feels broken to
     * anyone who has the habit.
     */
    fun select(index: Int) {
        if (index !in stacks.indices) return
        if (index == selectedIndex) {
            val stack = stacks[index]
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            return
        }
        selectedIndex = index
    }

    /**
     * Empties every tab and returns to the first one.
     *
     * For sign-out. Resetting only the visible tab leaves the previous user's screens sitting
     * behind the other three, which the next person to sign in on a shared device will find.
     */
    fun resetAll(rootKeys: List<AppNavKey>) {
        stacks.forEachIndexed { index, stack ->
            stack.clear()
            stack.add(rootKeys.getOrElse(index) { rootKeys.first() })
        }
        selectedIndex = 0
    }

    /**
     * What Back should do, given where we are.
     *
     * Inside a tab it pops. At a tab root it moves to the first tab, which is the behaviour
     * Android's own guidance describes and what makes the hardware Back predictable in a tabbed
     * app. Only at the root of the first tab is it the application's problem.
     */
    internal fun handleBack(onExitRequested: () -> Unit) {
        val stack = stacks[selectedIndex]
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            selectedIndex != 0 -> selectedIndex = 0
            else -> onExitRequested()
        }
    }
}

/**
 * Per-tab stacks that survive process death.
 *
 * Every tab's stack is serialised, not just the visible one — coming back from a kill to find the
 * other three tabs reset is the same lost-place problem the per-tab design exists to avoid.
 */
@Composable
fun rememberShellState(
    tabs: List<ShellTab>,
    serialization: NavKeySerialization,
): ShellState {
    val saver = remember(tabs.size, serialization) { shellSaver(tabs, serialization) }
    return rememberSaveable(tabs.size, saver = saver) {
        ShellState(
            stacks = tabs.map { tab -> mutableStateListOf(tab.key) },
            initialTab = 0,
        )
    }
}

/**
 * Saved as `selectedIndex|[[…],[…]]`.
 *
 * A hand-joined string rather than a wrapper `@Serializable` type, because the stacks are already
 * polymorphic and wrapping them means annotating the element type through two levels of `List` —
 * more ceremony than one separator, and one more thing to get wrong when a key is added.
 */
private fun shellSaver(
    tabs: List<ShellTab>,
    serialization: NavKeySerialization,
): Saver<ShellState, String> {
    val stacksSerializer = ListSerializer(ListSerializer(PolymorphicSerializer(AppNavKey::class)))

    return Saver(
        save = { state ->
            runCatching {
                val stacks = serialization.json.encodeToString(
                    stacksSerializer,
                    state.stacks.map { it.toList() },
                )
                "${state.selectedIndex}|$stacks"
            }.getOrNull()
        },
        restore = { encoded ->
            val separator = encoded.indexOf('|')
            val savedIndex = encoded.take(separator.coerceAtLeast(0)).toIntOrNull()
            val savedStacks = runCatching {
                serialization.json.decodeFromString(stacksSerializer, encoded.substring(separator + 1))
            }.getOrNull()

            // A tab added or removed since the bundle was written makes the saved shape wrong, and
            // an app update between the kill and the restore is exactly when that happens. Starting
            // fresh loses the user's place once; restoring a mismatched stack would crash.
            val usable = savedStacks?.takeIf { it.size == tabs.size }

            ShellState(
                stacks = tabs.mapIndexed { index, tab ->
                    val saved = usable?.getOrNull(index)?.takeIf { it.isNotEmpty() }
                    mutableStateListOf<AppNavKey>().apply { addAll(saved ?: listOf(tab.key)) }
                },
                initialTab = savedIndex?.takeIf { usable != null }?.coerceIn(tabs.indices) ?: 0,
            )
        },
    )
}

/**
 * The tabbed shell: a persistent bar, and the current tab's stack above it.
 *
 * ## The bar is hosted above the display, not inside a screen
 *
 * A bar that is part of each tab's screen is torn down and rebuilt on every switch, which makes
 * the badge flicker and lets the bar animate in with the content behind it. Here it is a sibling
 * of the display and is simply never recomposed by a tab change.
 *
 * ## It hides itself off a tab root
 *
 * A detail screen pushed from a tab is not a tab, and leaving the bar up invites the user to
 * switch away mid-task with no way back to where they were. [alwaysShowBar] exists for the
 * designs that disagree.
 */
@Composable
fun AppShell(
    tabs: List<ShellTab>,
    state: ShellState,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    alwaysShowBar: Boolean = false,
    onExitRequested: () -> Unit = {},
) {
    require(tabs.isNotEmpty()) { "AppShell needs at least one tab." }

    val backStack = state.current

    // Commands are applied to whichever tab is in front. A ViewModel inside a tab has no idea it
    // is in one, which is what keeps features unaware of the shell entirely.
    LaunchedEffect(navigator, state.selectedIndex) {
        navigator.commands.collect { command -> state.current.apply(command) }
    }

    val onRoot = backStack.size == 1
    val showBar = alwaysShowBar || onRoot

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AppNavHost(
                backStack = backStack,
                registry = registry,
                onBack = { state.handleBack(onExitRequested) },
            )
        }

        if (showBar) {
            AppBottomBar(
                items = tabs.map { tab ->
                    BottomNavItem(
                        label = tab.label,
                        icon = tab.icon,
                        selectedIcon = tab.selectedIcon,
                        badgeCount = tab.badgeCount,
                    )
                },
                selectedIndex = state.selectedIndex,
                onItemSelected = state::select,
            )
        }
    }
}
