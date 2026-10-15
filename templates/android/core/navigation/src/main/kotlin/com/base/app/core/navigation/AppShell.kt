package com.base.app.core.navigation

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.scene.SceneStrategy
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.common.navigation.FullScreenKey
import com.base.app.core.designsystem.component.navigation.BarActionScope
import com.base.app.core.designsystem.component.navigation.BarMinimize
import com.base.app.core.designsystem.component.navigation.TabLabels
import com.base.app.core.designsystem.component.navigation.rememberAppTabBarState
import com.base.app.core.designsystem.foundation.layout.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.back.ShellRootBack
import com.base.app.core.navigation.back.Verdict
import com.base.app.core.navigation.back.submit
import com.base.app.core.navigation.presentation.PresentationHost
import com.base.app.core.navigation.shell.BottomChromeWriter
import com.base.app.core.navigation.shell.BottomEdgeFade
import com.base.app.core.navigation.shell.ShellBar
import com.base.app.core.navigation.shell.ShellChrome
import com.base.app.core.navigation.shell.ShellShortcuts
import com.base.app.core.navigation.shell.chromeFor
import com.base.app.core.navigation.shell.fillGap
import com.base.app.core.navigation.shell.followSlide
import com.base.app.core.navigation.shell.rememberChromeSlide
import com.base.app.core.navigation.shell.sidebarAvailable
import com.base.app.core.navigation.shell.tabs.TabbedNavHost
import com.base.app.core.navigation.transition.ScreenMotion
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
 * The shell is the only writer of [LocalBottomChrome]: the bar's settled height, the keyboard while
 * it is up, or the screen-level bar of a screen that hides the tab bar, written only once the
 * screens have stopped moving.
 *
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
    sheetStrategy: SceneStrategy<AppNavKey>? = null,
    trailingAction: (@Composable BarActionScope.() -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    require(tabs.isNotEmpty()) { "AppShell needs at least one tab." }

    // A ViewModel inside a tab has no idea it is in one; commands go to the tab in front, or to the
    // presentation stack, after whatever is moving has settled.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(navigator, state, lifecycle) {
        navigator.applyWhileStarted(lifecycle) { command ->
            while (state.submit(command) == Verdict.Wait) {
                snapshotFlow { state.moving }.first { !it }
            }
        }
    }

    val motion = remember(config.screenResponse) { config.screenMotion() }
    val hidesBar = remember(config.hideBarOnPush, state) {
        { key: AppNavKey -> key is FullScreenKey || (config.hideBarOnPush && key !in state.rootKeys) }
    }
    val window = AppTheme.windowClass
    val chrome = chromeFor(window.width, window.height, state.sidebarOpen)
    SideEffect { state.useChrome(window, chrome) }
    val labels = tabs.map { stringResource(it.label) }
    val barState = rememberAppTabBarState()
    val bar = chrome == NavigationChrome.Bar
    val minimizes = bar && config.barMinimize == BarMinimize.OnScrollDown
    val slide = rememberChromeSlide()
    val toggleSidebar = if (sidebarAvailable(window.width, window.height)) slide.toggle(state, chrome) else null

    ShellRootBack(state)
    BottomChromeWriter(state, barState, wide = !bar)
    ShellShortcuts(labels, state, toggleSidebar)

    ShellChrome(
        modifier = modifier.fillMaxSize(),
        chrome = {
            if (bar) {
                BottomEdgeFade(
                    anchor = state.barAnchor,
                    parallax = motion.parallax,
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
                    onToggleSidebar = toggleSidebar,
                    modifier = Modifier.fillGap(slide),
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
