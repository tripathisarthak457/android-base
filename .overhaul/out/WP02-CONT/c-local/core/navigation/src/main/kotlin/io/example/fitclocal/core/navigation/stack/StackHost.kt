package io.example.fitclocal.core.navigation.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import io.example.fitclocal.core.common.navigation.AppNavKey
import io.example.fitclocal.core.designsystem.foundation.adaptive.WindowWidth
import io.example.fitclocal.core.designsystem.foundation.layout.ScreenChrome
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.navigation.AppBackStack
import io.example.fitclocal.core.navigation.LocalNavigationSettled
import io.example.fitclocal.core.navigation.NavPane
import io.example.fitclocal.core.navigation.NavRegistry
import io.example.fitclocal.core.navigation.NavTransitionStyle
import io.example.fitclocal.core.navigation.back.GateSettle
import io.example.fitclocal.core.navigation.back.StackBackHandler
import io.example.fitclocal.core.navigation.back.StackChange
import io.example.fitclocal.core.navigation.shell.BarAnchor
import io.example.fitclocal.core.navigation.transition.ScreenMotion

/**
 * One navigation host: [stack] drawn by a [StackScene], which keeps the screens under the top
 * composed and laid out and slides between them itself, the Gmail-style back card on the back
 * gesture, and the [io.example.fitclocal.core.navigation.back.BackGate] in front of Back.
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
    screens: ScreenChromes? = null,
) {
    val gate = stack.gate
    val pop = remember(stack) { { count: Int -> stack.pop(count) } }
    val provider = rememberEntryProvider(registry, hidesBar, screens)
    val backStack = stack.entries.toList()
    val entries = rememberDecoratedNavEntries(
        entries = remember(backStack, provider) { backStack.toNavEntries(provider) },
        entryDecorators = rememberEntryDecorators(),
    )
    val scene = remember(stack) { StackScene(gate) }
    val window = AppTheme.windowClass
    val columns = when {
        window.width >= WindowWidth.Large -> 3
        window.hasRoomForTwoPanes -> 2
        else -> 1
    }
    val reduceMotion = AppTheme.reduceMotion
    val keys = entries.map { it.contentKey }
    val plan = remember(keys, columns, reduceMotion) {
        val gestured = gate.lastChange == StackChange.GesturePop
        planChange(scene.applied, keys, entries.map { it.pane }, columns) { key ->
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
            anchor.attach(follower, scene::barCutout)
            onDispose { anchor.detach(follower) }
        }
    }

    val settled = remember(gate) { { !gate.moving } }
    CompositionLocalProvider(LocalNavigationSettled provides settled) {
        StackLayout(scene, plan, shown, AppTheme.layout.listPaneWidth, modifier)
    }
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
    screens: ScreenChromes?,
): (AppNavKey, String) -> NavEntry<AppNavKey> = remember(registry, hidesBar, screens) {
    { key, contentKey ->
        val destination = registry.destinationFor(key)
        val hides = hidesBar(key)
        val metadata = mapOf(
            KEY_METADATA to key,
            PANE_METADATA_KEY to destination.pane,
            HIDES_BAR_METADATA to hides,
            TRANSITION_METADATA to destination.transition,
        )
        NavEntry(key = key, contentKey = contentKey, metadata = metadata) { shown ->
            EntryFrame(contentKey, hides, screens) { destination.content(shown) }
        }
    }
}

/**
 * What the screens of one host report to the shell, by content key, for it to draw the window's
 * chrome for the one in front: a key in the stack twice is two screens, each with its own report.
 */
@Stable
internal class ScreenChromes {
    private val byContentKey = mutableStateMapOf<String, ScreenChrome>()

    operator fun get(contentKey: String?): ScreenChrome? = contentKey?.let { byContentKey[it] }

    fun register(contentKey: String, screen: ScreenChrome) {
        byContentKey[contentKey] = screen
    }

    fun unregister(contentKey: String, screen: ScreenChrome) {
        if (byContentKey[contentKey] === screen) byContentKey.remove(contentKey)
    }
}

/** A report for the screen of [contentKey], registered with [screens] while the screen is composed. */
@Composable
internal fun rememberScreenChrome(screens: ScreenChromes?, contentKey: String): ScreenChrome {
    val own = remember { ScreenChrome() }
    if (screens != null) {
        DisposableEffect(screens, contentKey, own) {
            screens.register(contentKey, own)
            onDispose { screens.unregister(contentKey, own) }
        }
    }
    return own
}

/**
 * How many pages under the top stay composed and laid out. Deeper ones are composed again, from
 * their saved state, when a pop reaches them; a burst of Back presses rarely goes further.
 */
private const val PARKED_DEPTH = 3

internal const val KEY_METADATA = "io.example.fitclocal.navigation.key"
internal const val HIDES_BAR_METADATA = "io.example.fitclocal.navigation.hidesBar"
internal const val PANE_METADATA_KEY = "io.example.fitclocal.navigation.pane"
private const val TRANSITION_METADATA = "io.example.fitclocal.navigation.transition"

internal val NavEntry<*>.navKey: AppNavKey get() = metadata.getValue(KEY_METADATA) as AppNavKey

internal val NavEntry<*>.hidesBar: Boolean get() = metadata[HIDES_BAR_METADATA] == true

internal val NavEntry<*>.pane: NavPane get() = metadata[PANE_METADATA_KEY] as? NavPane ?: NavPane.Single

internal val NeverHidesBar: (AppNavKey) -> Boolean = { false }
