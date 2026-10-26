package com.example.finalafter.crui2.core.navigation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.scene.SceneStrategy
import com.example.finalafter.crui2.core.common.navigation.AppNavKey
import com.example.finalafter.crui2.core.common.navigation.FullScreenKey
import com.example.finalafter.crui2.core.designsystem.component.navigation.AppTabBarDefaults
import com.example.finalafter.crui2.core.designsystem.component.navigation.BarActionScope
import com.example.finalafter.crui2.core.designsystem.component.navigation.BarMinimize
import com.example.finalafter.crui2.core.designsystem.component.navigation.TabLabels
import com.example.finalafter.crui2.core.designsystem.component.navigation.rememberAppTabBarState
import com.example.finalafter.crui2.core.designsystem.foundation.adaptive.LocalWindowMetrics
import com.example.finalafter.crui2.core.designsystem.foundation.layout.LocalTabBarVisible
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme
import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.navigation.NavRegistry
import com.example.finalafter.crui2.core.navigation.applyWhileStarted
import com.example.finalafter.crui2.core.navigation.back.Verdict
import com.example.finalafter.crui2.core.navigation.back.submit
import com.example.finalafter.crui2.core.navigation.shell.back.ShellRootBack
import com.example.finalafter.crui2.core.navigation.shell.presentation.PresentationHost
import com.example.finalafter.crui2.core.navigation.shell.tabs.TabbedNavHost
import com.example.finalafter.crui2.core.navigation.shell.transition.ScreenMotion
import kotlinx.coroutines.flow.first

/**
 * How the shell behaves, app-wide.
 *
 * @param hideBarOnPush every pushed screen hides the tab bar, which travels out with its tab's root
 *   and back with it (iOS `hidesBottomBarWhenPushed`). Off, the bar stays at every depth except over
 *   a [FullScreenKey] destination, which always hides it.
 * @param screenResponse overrides the screen spring's response, in seconds, to compare candidates
 *   on a device; null uses the theme's.
 */
@Immutable
data class ShellConfig(
    val hideBarOnPush: Boolean = false,
    val barLabels: TabLabels = TabLabels.Always,
    val barMinimize: BarMinimize = BarMinimize.Never,
    val screenResponse: Float? = null,
) {
    internal fun screenMotion(): ScreenMotion = ScreenMotion(response = screenResponse ?: ScreenMotion.DEFAULT_RESPONSE)
}

/**
 * The signed-in app: every tab's stack, the navigation chrome for the window (the floating tab bar
 * on compact widths, a rail on wider or short windows, a sidebar on large ones; see
 * [NavigationChrome]), the window's keyboard shortcuts, and the presentation host above it all.
 *
 * The tab host is called from exactly one place whatever the window: a call from another branch
 * would be another composition, and a fold, a rotation or a resize would rebuild every tab's
 * screens and clear their ViewModels. The size class changes only the chrome.
 *
 * The shell is the only writer of the window's chrome holders, written only once the screens have
 * stopped moving: [LocalBottomChrome] (the bar's settled height, the keyboard while it is up, or the
 * screen-level bar of a screen that hides the tab bar) and [LocalWindowTopChrome] (the top bar of
 * the screen in front). Every screen reports to it through its own `LocalScreenChrome`.
 *
 * @param receivesCommands false while this shell is leaving ([HostCrossfade]), so commands wait for
 *   the host that replaces it.
 * @param sheetStrategy draws sheet presentations (`Presentation.Sheet`); without one they are
 *   presented as covers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppShell(
    tabs: List<ShellTab>,
    state: ShellState,
    registry: NavRegistry,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    config: ShellConfig = ShellConfig(),
    receivesCommands: Boolean = true,
    sheetStrategy: SceneStrategy<AppNavKey>? = null,
    trailingAction: (@Composable BarActionScope.() -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    require(tabs.isNotEmpty()) { "AppShell needs at least one tab." }

    val window = AppTheme.windowClass
    val metrics = LocalWindowMetrics.current
    val chrome = chromeFor(window.width, window.height, state.sidebarOpen)
    // Written before anything below reads it, as rememberUpdatedState writes its value: the first
    // frame and the frame of every window change are composed with the chrome they are drawn with.
    // Written after composition instead, they would be composed with the previous one and again on
    // the next frame.
    state.useChrome(window, metrics, chrome)

    if (receivesCommands) {
        // A ViewModel inside a tab has no idea it is in one; commands go to the tab in front, or to
        // the presentation stack, after whatever is moving has settled.
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(navigator, state, lifecycle) {
            navigator.applyWhileStarted(lifecycle) { command ->
                while (state.submit(command) == Verdict.Wait) {
                    snapshotFlow { state.moving }.first { !it }
                }
            }
        }
    }

    val motion = remember(config.screenResponse) { config.screenMotion() }
    val hidesBar = remember(config.hideBarOnPush, state) {
        { key: AppNavKey -> key is FullScreenKey || (config.hideBarOnPush && key !in state.rootKeys) }
    }
    val labels = tabs.map { stringResource(it.label) }
    val barState = rememberAppTabBarState()
    val bar = chrome == NavigationChrome.Bar
    val minimizes = bar && config.barMinimize == BarMinimize.OnScrollDown
    val slide = rememberChromeSlide()
    val scrolledUnder = rememberScrolledUnder(state)
    val toggleSidebar = if (sidebarAvailable(window.width, window.height)) slide.toggle(state, chrome) else null

    ShellRootBack(state)
    WindowChromeWriter(state, barState, wide = !bar)
    ShellShortcuts(labels, state, toggleSidebar)

    Box(modifier.fillMaxSize()) {
        ShellChrome(
            modifier = Modifier.fillMaxSize(),
            chrome = {
                if (bar) {
                    BottomEdgeFade(
                        state = state,
                        parallax = motion.parallax,
                        scrolledUnder = scrolledUnder,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    ShellBar(
                        chrome = chrome,
                        tabs = tabs,
                        labels = labels,
                        state = state,
                        barState = barState,
                        config = config,
                        trailingAction = trailingAction,
                        accessory = accessory,
                        motion = motion,
                        scrolledUnder = scrolledUnder,
                        onToggleSidebar = null,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                if (!bar) {
                    ShellBar(
                        chrome = chrome,
                        tabs = tabs,
                        labels = labels,
                        state = state,
                        barState = barState,
                        config = config,
                        trailingAction = trailingAction,
                        accessory = accessory,
                        motion = motion,
                        scrolledUnder = scrolledUnder,
                        onToggleSidebar = toggleSidebar,
                        modifier = Modifier.fillGap(slide, AppTabBarDefaults.surface()),
                    )
                }
                CompositionLocalProvider(LocalTabBarVisible provides bar) {
                    TabbedNavHost(
                        tabs = tabs,
                        labels = labels,
                        state = state,
                        registry = registry,
                        motion = motion,
                        hidesBar = hidesBar,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // Beside a rail or a sidebar a sliding screen must not draw over it, and
                            // the chrome already clears the cutout on its side.
                            .then(
                                if (bar) {
                                    Modifier
                                } else {
                                    Modifier
                                        .followSlide(slide)
                                        .clipToBounds()
                                        .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                                },
                            )
                            .then(if (minimizes) Modifier.nestedScroll(barState.nestedScrollConnection) else Modifier),
                    )
                }
            }
        }
        PresentationHost(state = state, registry = registry, motion = motion, sheetStrategy = sheetStrategy)
    }
}
