package com.base.app.core.navigation.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.foundation.ChromeInset
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.rememberReduceMotion
import com.base.app.core.navigation.AppBackStack
import com.base.app.core.navigation.NavPane
import com.base.app.core.navigation.NavRegistry
import com.base.app.core.navigation.NavTransitionStyle
import com.base.app.core.navigation.back.GateSettle
import com.base.app.core.navigation.back.StackBackHandler
import com.base.app.core.navigation.back.StackChange
import com.base.app.core.navigation.shell.BarAnchor
import com.base.app.core.navigation.transition.ScreenMotion

/**
 * One navigation host: [stack] drawn by a [StackScene], which keeps the screens under the top
 * composed and laid out and slides between them itself, the Gmail-style back card on the back
 * gesture, and the [com.base.app.core.navigation.back.BackGate] in front of Back.
 *
 * Navigation 3 decorates the entries (saved state and a ViewModel store per entry); the scene, not
 * `NavDisplay`, draws them, because `NavDisplay` moves an entry into a new scene on every push and
 * pop, and the move detaches it.
 */
@Composable
internal fun StackHost(
    stack: AppBackStack,
    registry: NavRegistry,
    motion: ScreenMotion,
    modifier: Modifier = Modifier,
    hidesBar: (AppNavKey) -> Boolean = NeverHidesBar,
    anchor: BarAnchor? = null,
    screenBars: ScreenBars? = null,
) {
    val gate = stack.gate
    val pop = remember(stack) { { count: Int -> stack.pop(count) } }
    val entries = rememberDecoratedNavEntries(
        backStack = stack.entries,
        entryDecorators = rememberEntryDecorators(),
        entryProvider = rememberEntryProvider(registry, hidesBar, screenBars),
    )
    val scene = remember(stack) { StackScene(gate) }
    val twoPanes = AppTheme.windowSize.hasRoomForTwoPanes
    val reduceMotion = rememberReduceMotion()
    val keys = entries.map { it.contentKey }
    val plan = remember(keys, twoPanes, reduceMotion) {
        val gestured = gate.lastChange == StackChange.GesturePop
        planChange(scene.applied, keys, entries.map { it.pane }, twoPanes) { key ->
            !reduceMotion && !gestured && scene.slidesOf(key, entries)
        }
    }
    val kept = plan.pages.takeLast(PARKED_DEPTH + 1).flatMapTo(HashSet()) { it.members }
    val shown = entries.filter { it.contentKey in kept } + scene.departing(plan, keys.toSet())
    val scope = rememberCoroutineScope()
    SideEffect {
        scene.motion = motion
        scene.apply(plan, entries, shown.filter { it.hidesBar }.mapTo(HashSet()) { it.contentKey }, scope)
    }
    if (anchor != null) {
        DisposableEffect(anchor, scene) {
            val follower = scene::barCover
            anchor.attach(follower)
            onDispose { anchor.detach(follower) }
        }
    }

    StackLayout(scene, plan, shown, AppTheme.layout.listPaneWidth, modifier)
    StackBackHandler(stack, scene, pop, cardEnabled = !reduceMotion)
    GateSettle(gate, pop)
}

private fun StackScene.slidesOf(key: Any, entries: List<NavEntry<AppNavKey>>): Boolean {
    val entry = entries.firstOrNull { it.contentKey == key } ?: appliedEntry(key) ?: return true
    return entry.metadata[TRANSITION_METADATA] != NavTransitionStyle.None
}

@Composable
private fun rememberEntryDecorators(): List<NavEntryDecorator<AppNavKey>> {
    val saveable = rememberSaveableStateHolderNavEntryDecorator<AppNavKey>()
    val viewModels = rememberViewModelStoreNavEntryDecorator<AppNavKey>()
    return remember(saveable, viewModels) { listOf(saveable, viewModels) }
}

@Composable
private fun rememberEntryProvider(
    registry: NavRegistry,
    hidesBar: (AppNavKey) -> Boolean,
    screenBars: ScreenBars?,
): (AppNavKey) -> NavEntry<AppNavKey> = remember(registry, hidesBar, screenBars) {
    { key ->
        val destination = registry.destinationFor(key)
        val hides = hidesBar(key)
        val metadata = mapOf(
            KEY_METADATA to key,
            PANE_METADATA_KEY to destination.pane,
            HIDES_BAR_METADATA to hides,
            TRANSITION_METADATA to destination.transition,
        )
        NavEntry(key = key, metadata = metadata) { shown ->
            EntryFrame(shown, hides, screenBars) { destination.content(shown) }
        }
    }
}

/** The screen-level bottom bars of one host's screens, for the shell to report the one in front. */
@Stable
internal class ScreenBars {
    val heights = mutableStateMapOf<AppNavKey, ChromeInset>()
}

/**
 * How many pages under the top stay composed and laid out. Deeper ones are composed again, from
 * their saved state, when a pop reaches them; a burst of Back presses rarely goes further.
 */
private const val PARKED_DEPTH = 3

internal const val KEY_METADATA = "com.base.app.navigation.key"
internal const val HIDES_BAR_METADATA = "com.base.app.navigation.hidesBar"
internal const val PANE_METADATA_KEY = "com.base.app.navigation.pane"
private const val TRANSITION_METADATA = "com.base.app.navigation.transition"

internal val NavEntry<*>.navKey: AppNavKey get() = metadata.getValue(KEY_METADATA) as AppNavKey

internal val NavEntry<*>.hidesBar: Boolean get() = metadata[HIDES_BAR_METADATA] == true

internal val NavEntry<*>.pane: NavPane get() = metadata[PANE_METADATA_KEY] as? NavPane ?: NavPane.Single

internal val NeverHidesBar: (AppNavKey) -> Boolean = { false }
