package com.base.app.core.navigation.shell

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.base.app.core.designsystem.component.navigation.AppTabBar
import com.base.app.core.designsystem.component.navigation.AppTabBarState
import com.base.app.core.designsystem.component.navigation.BarActionScope
import com.base.app.core.designsystem.component.navigation.BarOrientation
import com.base.app.core.designsystem.component.navigation.TabItem
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.ShellConfig
import com.base.app.core.navigation.ShellState
import com.base.app.core.navigation.ShellTab
import com.base.app.core.navigation.transition.coverScrimAlpha

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShellBar(
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
                .followCovered(state.anchors[state.selectedIndex], parallax, coverScrimAlpha(AppTheme.colors.isLight))
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
