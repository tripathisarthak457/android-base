package com.example.cmp.core.navigation.shell

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.node.invalidateSemantics
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.example.cmp.core.designsystem.component.navigation.AppSidebar
import com.example.cmp.core.designsystem.component.navigation.AppSidebarDefaults
import com.example.cmp.core.designsystem.component.navigation.AppTabBar
import com.example.cmp.core.designsystem.component.navigation.AppTabBarState
import com.example.cmp.core.designsystem.component.navigation.BarActionScope
import com.example.cmp.core.designsystem.component.navigation.BarOrientation
import com.example.cmp.core.designsystem.component.navigation.TabItem
import com.example.cmp.core.designsystem.foundation.adaptive.WindowWidth
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.navigation.shell.stack.coverScrimAlpha
import com.example.cmp.core.navigation.shell.transition.ScreenMotion
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * The tabs in the window's chrome: the floating bar, the rail or the sidebar. When the chrome has
 * no room for every tab, the last place is More, which is shown selected while its list or a tab it
 * lists is in front.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShellBar(
    chrome: NavigationChrome,
    tabs: List<ShellTab>,
    labels: List<String>,
    state: ShellState,
    barState: AppTabBarState,
    config: ShellConfig,
    trailingAction: (@Composable BarActionScope.() -> Unit)?,
    accessory: (@Composable () -> Unit)?,
    motion: ScreenMotion,
    scrolledUnder: () -> Float,
    onToggleSidebar: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val shown = chrome.tabsShown(tabs.size)
    val hasMore = shown < tabs.size
    val sections = tabs.map { tab -> tab.section?.let { stringResource(it) } }
    val moreLabel = stringResource(R.string.shell_more)
    val items = remember(tabs, labels, sections, shown, moreLabel) {
        val visible = tabs.take(shown).mapIndexed { index, tab ->
            TabItem(labels[index], tab.icon, tab.selectedIcon, tab.badgeCount, sections[index])
        }
        if (hasMore) {
            visible +
                TabItem(
                    moreLabel,
                    AppIcons.MoreHorizontal,
                    badgeCount = tabs.drop(shown).sumOf {
                        it.badgeCount
                    },
                )
        } else {
            visible
        }
    }
    val selected = if (hasMore &&
        (state.moreShown || state.selectedIndex >= shown)
    ) {
        shown
    } else {
        state.selectedIndex
    }
    val onSelect: (Int) -> Unit = { index ->
        if (hasMore &&
            index == shown
        ) {
            state.showMore()
        } else {
            state.select(index)
        }
    }
    val sideHeader: (@Composable BarActionScope.() -> Unit)? =
        if (onToggleSidebar == null && trailingAction == null) {
            null
        } else {
            {
                if (onToggleSidebar != null) {
                    val label = if (chrome ==
                        NavigationChrome.Sidebar
                    ) {
                        R.string.shell_hide_sidebar
                    } else {
                        R.string.shell_show_sidebar
                    }
                    Action(
                        AppIcons.Menu,
                        stringResource(label),
                        onToggleSidebar,
                        tag = "sidebar_toggle",
                    )
                }
                trailingAction?.invoke(this)
            }
        }
    when (chrome) {
        NavigationChrome.Bar -> AppTabBar(
            items = items,
            selected = selected,
            onSelect = onSelect,
            onReselect = onSelect,
            modifier = modifier
                .testTag("tab_bar")
                .followCovered(
                    state.barAnchor,
                    motion.parallax,
                    coverScrimAlpha(AppTheme.colors.isLight),
                )
                .hideUnderKeyboard(WindowInsets.ime, motion.fraction)
                .parkWhenCovered(state.barAnchor),
            labels = config.barLabels,
            minimize = config.barMinimize,
            state = barState,
            scrolledUnder = scrolledUnder,
            trailingAction = trailingAction,
            accessory = accessory,
        )

        NavigationChrome.Rail -> AppTabBar(
            items = items,
            selected = selected,
            onSelect = onSelect,
            onReselect = onSelect,
            modifier = modifier.testTag("rail"),
            orientation = BarOrientation.Vertical,
            labels = config.barLabels,
            state = barState,
            trailingAction = sideHeader,
            accessory = accessory,
        )

        NavigationChrome.Sidebar -> AppSidebar(
            items = items,
            selected = selected,
            onSelect = onSelect,
            onReselect = onSelect,
            modifier = modifier.testTag("sidebar"),
            width = sidebarWidth(),
            header = sideHeader,
            footer = accessory,
        )
    }
}

/** The sidebar's width for the window: wider on the widest windows. */
@Composable
internal fun sidebarWidth(): Dp = if (AppTheme.windowClass.width ==
    WindowWidth.ExtraLarge
) {
    AppSidebarDefaults.WideWidth
} else {
    AppSidebarDefaults.Width
}

/**
 * The keyboard rises over the bar: the bar moves down with it in the layout phase and is
 * transparent once the keyboard covers it, so it never floats above the keyboard's top edge.
 *
 * Before Android 11 the system does not animate the keyboard's inset, which jumps to its full
 * height in one frame; there the bar sinks on [spring] instead of jumping with it.
 */
private fun Modifier.hideUnderKeyboard(ime: WindowInsets, spring: AnimationSpec<Float>): Modifier =
    this then HideUnderKeyboardElement(ime, spring)

private data class HideUnderKeyboardElement(
    val ime: WindowInsets,
    val spring: AnimationSpec<Float>,
) : ModifierNodeElement<HideUnderKeyboardNode>() {
    override fun create() = HideUnderKeyboardNode(ime, spring)

    override fun update(node: HideUnderKeyboardNode) {
        node.ime = ime
        node.spring = spring
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "hideUnderKeyboard"
    }
}

private class HideUnderKeyboardNode(var ime: WindowInsets, var spring: AnimationSpec<Float>) :
    Modifier.Node(),
    LayoutModifierNode {

    private val animated = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    /** How far the bar has sunk where the inset jumps: 0 in place, 1 under the keyboard. */
    private val sunk = Animatable(0f)

    override fun onAttach() {
        if (animated) return
        val density = requireDensity()
        coroutineScope.launch {
            snapshotFlow {
                ime.getBottom(density) > 0
            }.collect { up -> sunk.animateTo(if (up) 1f else 0f, spring) }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            val keyboard = if (animated) {
                ime.getBottom(this@measure).coerceAtMost(placeable.height)
            } else {
                (placeable.height * sunk.value).roundToInt()
            }
            placeable.placeWithLayer(0, keyboard) {
                alpha =
                    if (keyboard >= placeable.height) 0f else 1f
            }
        }
    }
}

/**
 * Fully covered by a screen that hides it, the bar is parked: still laid out, with its layers, but
 * drawn at alpha 0 off the window, out of reach of touch and out of the semantics tree. A pop or a
 * back gesture that brings it back changes two layer properties; nothing is composed, measured
 * or recorded. All of it follows [anchor] in the layer and semantics phases, so it recomposes
 * nothing either.
 */
private fun Modifier.parkWhenCovered(anchor: BarAnchor): Modifier =
    this then ParkWhenCoveredElement(anchor)

private data class ParkWhenCoveredElement(val anchor: BarAnchor) :
    ModifierNodeElement<ParkWhenCoveredNode>() {
    override fun create() = ParkWhenCoveredNode(anchor)

    override fun update(node: ParkWhenCoveredNode) {
        node.anchor = anchor
        node.follow()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "parkWhenCovered"
    }
}

private class ParkWhenCoveredNode(var anchor: BarAnchor) :
    Modifier.Node(),
    LayoutModifierNode,
    SemanticsModifierNode,
    ObserverModifierNode {

    private var parked = false

    private val layer: GraphicsLayerScope.() -> Unit = {
        if (anchor.progress() >= 1f) {
            alpha = 0f
            translationX = -2f * size.width
        }
    }

    override val shouldClearDescendantSemantics: Boolean get() = parked

    override fun SemanticsPropertyReceiver.applySemantics() = Unit

    override fun onAttach() = follow()

    override fun onObservedReadsChanged() = follow()

    fun follow() {
        observeReads {
            val covered = anchor.progress() >= 1f
            if (covered != parked) {
                parked = covered
                invalidateSemantics()
            }
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layer)
        }
    }
}
