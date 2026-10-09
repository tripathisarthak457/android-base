package com.base.app.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.scene.SceneStrategy
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.base.app.core.designsystem.component.navigation.AppTabBar
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.component.navigation.BarActionScope
import com.base.app.core.designsystem.component.navigation.BarMinimize
import com.base.app.core.designsystem.component.navigation.BarOrientation
import com.base.app.core.designsystem.component.navigation.TabItem
import com.base.app.core.designsystem.component.navigation.TabLabels
import com.base.app.core.designsystem.component.navigation.rememberAppTabBarState
import com.base.app.core.designsystem.foundation.LocalBottomChrome
import com.base.app.core.designsystem.foundation.LocalTabBarVisible
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.WindowWidth
import kotlinx.coroutines.flow.first

/**
 * How the shell behaves, app-wide.
 *
 * @param hideBarOnPush every pushed screen hides the tab bar, which travels out with its tab's root
 *   and back with it (iOS `hidesBottomBarWhenPushed`). Off, the bar stays at every depth.
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
 * The signed-in app: every tab's stack, the tab bar along the bottom of compact windows or a rail
 * beside wide ones, and the presentation host above both.
 *
 * The tab host is called from exactly one place whatever the window: a call from another branch
 * would be another composition, and a fold, a rotation or a resize would rebuild every tab's
 * screens and clear their ViewModels. The width class changes only the chrome.
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
        { key: AppNavKey -> config.hideBarOnPush && key !in state.rootKeys }
    }
    val wide = AppTheme.windowSize.width >= WindowWidth.Medium
    val barState = rememberAppTabBarState()
    val minimizes = !wide && config.barMinimize == BarMinimize.OnScrollDown

    ShellRootBack(state)
    BottomChromeWriter(state, barState, wide)

    ShellChrome(
        modifier = modifier.fillMaxSize(),
        chrome = {
            if (!wide) {
                ShellBar(
                    tabs = tabs,
                    state = state,
                    barState = barState,
                    config = config,
                    trailingAction = trailingAction,
                    accessory = accessory,
                    parallax = motion.parallax,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        },
    ) {
        Row(Modifier.fillMaxSize()) {
            if (wide) ShellBar(tabs, state, barState, config, trailingAction = null, accessory = null, parallax = 0f, vertical = true)
            CompositionLocalProvider(LocalTabBarVisible provides !wide) {
                TabbedNavHost(
                    state = state,
                    registry = registry,
                    motion = motion,
                    hidesBar = hidesBar,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        // Beside a rail a sliding screen must not draw over it; the rail also already
                        // clears the cutout on its side.
                        .then(
                            if (wide) {
                                Modifier.clipToBounds().consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                            } else {
                                Modifier
                            },
                        )
                        .then(if (minimizes) Modifier.nestedScroll(barState.nestedScrollConnection) else Modifier),
                )
            }
        }
    }
    PresentationHost(state = state, registry = registry, motion = motion, sheetStrategy = sheetStrategy)
}

/**
 * The shell's layers: content, and the chrome drawn over it. A plain box; with liquid glass on,
 * this is where the glass stage records the content for the chrome to sample.
 */
@Composable
internal fun ShellChrome(
    modifier: Modifier,
    chrome: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        content()
        chrome()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShellBar(
    tabs: List<ShellTab>,
    state: ShellState,
    barState: AppTabBarState,
    config: ShellConfig,
    trailingAction: (@Composable BarActionScope.() -> Unit)?,
    accessory: (@Composable () -> Unit)?,
    parallax: Float,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
) {
    val labels = tabs.map { stringResource(it.label) }
    val items = remember(tabs, labels) {
        tabs.mapIndexed { index, tab -> TabItem(labels[index], tab.icon, tab.selectedIcon, tab.badgeCount) }
    }
    // Fully covered by a screen that hides it, the bar stays composed but is not placed: nothing to
    // draw, touch or read out, and nothing to compose again in the first frame of the pop that
    // brings it back.
    val covered by remember(state) {
        derivedStateOf { state.anchors[state.selectedIndex].progress() >= 1f }
    }
    val focus = LocalFocusManager.current
    AppTabBar(
        items = items,
        selected = state.selectedIndex,
        onSelect = { index ->
            // A field focused in the tab being left would keep the keyboard up over the next one.
            focus.clearFocus()
            state.select(index)
        },
        onReselect = state::select,
        modifier = if (vertical) {
            modifier
        } else {
            modifier
                .followRoot(state.anchors[state.selectedIndex], parallax, coverScrimAlpha(AppTheme.colors.isLight))
                .hideUnderKeyboard()
                .then(if (covered) Parked else Modifier)
        },
        orientation = if (vertical) BarOrientation.Vertical else BarOrientation.Horizontal,
        labels = config.barLabels,
        minimize = config.barMinimize,
        state = barState,
        trailingAction = trailingAction,
        accessory = accessory,
    )
}

/**
 * The keyboard rises over the bar: the bar moves down with it in the layout phase and is
 * transparent once the keyboard covers it, so it never floats above the keyboard's top edge.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Modifier.hideUnderKeyboard(): Modifier {
    val ime = WindowInsets.ime
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val density = this
        layout(placeable.width, placeable.height) {
            val keyboard = ime.getBottom(density).coerceAtMost(placeable.height)
            placeable.placeWithLayer(0, keyboard) { alpha = if (keyboard >= placeable.height) 0f else 1f }
        }
    }
}

/** Measured, so the bar keeps its size, but neither placed nor in the semantics tree. */
private val Parked = Modifier
    .clearAndSetSemantics {}
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {}
    }

/** Back at the root of another tab selects the first tab; registered first, so asked last. */
@Composable
private fun ShellRootBack(state: ShellState) {
    val idle = rememberNavigationEventState(NavigationEventInfo.None)
    val enabled by remember(state) { derivedStateOf { state.canBackAtRoot } }
    NavigationBackHandler(state = idle, isBackEnabled = enabled, onBackCompleted = { state.backAtRoot() })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BottomChromeWriter(state: ShellState, barState: AppTabBarState, wide: Boolean) {
    val chrome = LocalBottomChrome.current
    val density = LocalDensity.current
    val keyboard = WindowInsets.imeAnimationTarget
    LaunchedEffect(state, barState, wide, chrome, density) {
        snapshotFlow {
            if (state.moving) {
                null
            } else {
                with(density) {
                    bottomChrome(
                        presented = state.presentedKeys.lastOrNull()?.let { state.presentedBars.heights[it]?.value },
                        keyboard = keyboard.getBottom(this).toDp(),
                        screenBar = state.current.current.takeIf { state.anchors[state.selectedIndex].progress() >= 1f }
                            ?.let { state.screenBars[state.selectedIndex].heights[it]?.value },
                        bar = if (wide) 0.dp else barState.settledHeight.toDp(),
                    )
                }
            }
        }.collect { height -> if (height != null) chrome.value = height }
    }
}

/**
 * The chrome over the bottom edge, in order of what covers what: a presented screen's own bottom
 * bar, the keyboard, a screen-level bar where the tab bar is hidden, the tab bar.
 */
internal fun bottomChrome(presented: Dp?, keyboard: Dp, screenBar: Dp?, bar: Dp): Dp = when {
    presented != null -> presented
    keyboard > 0.dp -> keyboard
    screenBar != null -> screenBar
    else -> bar
}
