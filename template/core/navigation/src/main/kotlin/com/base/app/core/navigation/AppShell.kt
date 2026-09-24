package com.base.app.core.navigation

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.navigation.AppBottomBar
import com.base.app.core.designsystem.component.navigation.AppBottomBarDefaults
import com.base.app.core.designsystem.component.navigation.AppNavigationRail
import com.base.app.core.designsystem.component.navigation.BottomNavItem
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.WindowWidth
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.builtins.ListSerializer

/**
 * One tab. [key] is the tab's root destination and doubles as its identity, so two tabs cannot
 * accidentally share a stack.
 */
data class ShellTab(
    val key: AppNavKey,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

/** The tab bar's state: one back stack per tab, and which one is in front. */
class ShellState internal constructor(
    internal val stacks: List<SnapshotStateList<AppNavKey>>,
    initialTab: Int,
) {
    var selectedIndex by mutableIntStateOf(initialTab)
        internal set

    /** The stack that is currently on screen. Every navigation command applies to this one. */
    val current: AppBackStack get() = AppBackStack(stacks[selectedIndex])

    /** Switches tabs, or — when the tab is already selected — returns it to its root. */
    fun select(index: Int) {
        if (index !in stacks.indices) return
        if (index == selectedIndex) {
            val stack = stacks[index]
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
            return
        }
        selectedIndex = index
    }

    /** Empties every tab and returns to the first one. */
    fun resetAll(rootKeys: List<AppNavKey>) {
        stacks.forEachIndexed { index, stack ->
            stack.clear()
            stack.add(rootKeys.getOrElse(index) { rootKeys.first() })
        }
        selectedIndex = 0
    }

    /** What Back should do, given where we are. Inside a tab it pops. */
    internal fun handleBack(onExitRequested: () -> Unit) {
        val stack = stacks[selectedIndex]
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            selectedIndex != 0 -> selectedIndex = 0
            else -> onExitRequested()
        }
    }
}

/** Per-tab stacks that survive process death. */
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

/** Saved as `selectedIndex|[[…],[…]]`. */
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

            // Start fresh if the tab count changed since this was saved; a mismatched restore would
            // crash.
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
 * The tabbed shell: the current tab's stack, with a bottom bar in a Compact-width window and a
 * navigation rail beside it from Medium up — a tablet, an unfolded foldable, a phone held
 * sideways, a desktop window.
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

    // Commands are applied to whichever tab is in front. A ViewModel inside a tab has no idea it
    // is in one, which is what keeps features unaware of the shell entirely.
    LaunchedEffect(navigator, state) {
        navigator.commands.collect { command -> state.current.apply(command) }
    }

    val onRoot = state.current.size == 1
    val showBar = alwaysShowBar || onRoot
    val motion = AppTheme.motion

    BackHandler(enabled = onRoot && state.selectedIndex != 0) {
        state.handleBack(onExitRequested)
    }

    val items = tabs.map { tab ->
        BottomNavItem(
            label = stringResource(tab.label),
            icon = tab.icon,
            selectedIcon = tab.selectedIcon,
            badgeCount = tab.badgeCount,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // A rail stays beside the content at every depth; nothing covers the bottom of the window,
        // so no entry needs the bar's inset.
        if (AppTheme.windowSize.width >= WindowWidth.Medium) {
            Row(modifier = Modifier.fillMaxSize()) {
                AppNavigationRail(
                    items = items,
                    selectedIndex = state.selectedIndex,
                    onItemSelected = state::select,
                )
                TabbedNavHost(
                    state = state,
                    registry = registry,
                    rootBottomInset = 0.dp,
                    insetEveryEntry = false,
                    onBack = { state.handleBack(onExitRequested) },
                    // The rail already clears the cutout on its side.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start)),
                )
            }
            return@Box
        }

        TabbedNavHost(
            state = state,
            registry = registry,
            rootBottomInset = AppBottomBarDefaults.occupiedHeight(),
            insetEveryEntry = alwaysShowBar,
            onBack = { state.handleBack(onExitRequested) },
            modifier = Modifier.fillMaxSize(),
        )

        AnimatedVisibility(
            visible = showBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(motion.sheet()) { it } + fadeIn(tween(motion.quick)),
            exit = slideOutVertically(tween(motion.medium, easing = motion.exit)) { it } +
                fadeOut(tween(motion.medium, easing = motion.exit)),
        ) {
            AppBottomBar(
                items = items,
                selectedIndex = state.selectedIndex,
                onItemSelected = state::select,
            )
        }
    }
}
