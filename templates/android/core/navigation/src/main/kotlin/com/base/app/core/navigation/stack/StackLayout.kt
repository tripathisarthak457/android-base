package com.base.app.core.navigation.stack

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.FrameRateCategory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.preferredFrameRate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import com.base.app.core.common.navigation.AppNavKey
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.feedback.AppEmptyState
import com.base.app.core.designsystem.foundation.LocalTabVisible
import com.base.app.core.designsystem.foundation.TabVisibility
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.shell.tabs.CappedLifecycle

/**
 * Lays out [shown] (the stack's entries plus any still sliding out) and the empty detail panes of
 * [plan] as children of one layout, each where its page puts it. Which of them is visible, and
 * where it is during a slide, is decided in their layers from [scene].
 */
@Composable
internal fun StackLayout(
    scene: StackScene,
    plan: StackPlan,
    shown: List<NavEntry<AppNavKey>>,
    listPaneWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val tab = LocalTabVisible.current
    val isLight = AppTheme.colors.isLight
    // Slides and the card move layers only, which cast no frame-rate vote of their own: without
    // one, a phone with an adaptive refresh rate may draw them at 60 Hz. Raised only while they
    // move, since the vote passes down to every layer of every screen in the stack.
    val rate = if (scene.moving || scene.card.active) FrameRateCategory.High else FrameRateCategory.Default
    Layout(
        modifier = modifier.preferredFrameRate(rate),
        content = {
            shown.forEach { entry ->
                key(entry.contentKey) {
                    EntrySlot(entry, scene, plan, onTop = entry.contentKey in plan.top.shown, tab, isLight)
                }
            }
            plan.emptyDetails.forEach { empty ->
                key(empty) { EmptyDetailSlot(empty, scene, isLight) }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        scene.width = width.toFloat()
        scene.height = height.toFloat()
        scene.rtl = layoutDirection == LayoutDirection.Rtl
        if (scene.density.density != density || scene.density.fontScale != fontScale) scene.density = Density(density, fontScale)
        val listWidth = listPaneWidth.roundToPx().coerceAtMost(width)
        val keys = shown.map { it.contentKey } + plan.emptyDetails
        val placeables = measurables.mapIndexed { index, measurable ->
            val paneWidth = when (plan.slotOf(keys[index])) {
                Slot.Full -> width
                Slot.List -> listWidth
                Slot.Detail -> width - listWidth
            }
            measurable.measure(Constraints.fixed(paneWidth, height))
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val x = if (plan.slotOf(keys[index]) == Slot.Detail) listWidth else 0
                scene.lefts[keys[index]] = (if (scene.rtl) width - x - placeable.width else x).toFloat()
                placeable.placeRelative(x, 0)
            }
        }
    }
}

@Composable
private fun EntrySlot(
    entry: NavEntry<AppNavKey>,
    scene: StackScene,
    plan: StackPlan,
    onTop: Boolean,
    tab: TabVisibility,
    isLight: Boolean,
) {
    val key = entry.contentKey
    val lifecycle = rememberEntryLifecycle(scene, key)
    val visibility = remember(scene, key, tab) { TabVisibility { tab.isVisible && key in scene.top } }
    val detailPane = plan.slotOf(key) == Slot.Detail
    val divider = AppTheme.colors.divider
    val draw = remember(scene, key, isLight, detailPane, divider) {
        Modifier
            .graphicsLayer { with(scene) { pose(key) } }
            .drawWithContent {
                with(scene) { drawOver(key, isLight) }
                if (detailPane) drawPaneDivider(divider)
            }
    }
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycle,
        LocalTabVisible provides visibility,
    ) {
        Box(draw.then(if (onTop) Modifier else Covered)) { entry.Content() }
    }
}

@Composable
private fun EmptyDetailSlot(key: EmptyDetail, scene: StackScene, isLight: Boolean) {
    val divider = AppTheme.colors.divider
    val draw = remember(scene, key, isLight, divider) {
        Modifier
            .graphicsLayer { with(scene) { pose(key) } }
            .drawWithContent {
                with(scene) { drawOver(key, isLight) }
                drawPaneDivider(divider)
            }
    }
    Box(draw) {
        AppEmptyState(
            title = stringResource(R.string.designsystem_nothing_selected),
            message = stringResource(R.string.designsystem_nothing_selected_message),
            icon = AppIcons.ListView,
        )
    }
}

private fun DrawScope.drawPaneDivider(color: Color) {
    val width = 1f.coerceAtLeast(density)
    val x = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width
    drawRect(color, Offset(x, 0f), Size(width, size.height))
}

/**
 * A covered or leaving screen: out of what accessibility reads, and out of keyboard focus. Its
 * layer already keeps it off the window, so it takes no touch.
 */
private val Covered = Modifier
    .clearAndSetSemantics {}
    .focusProperties { onEnter = { cancelFocusChange() } }
    .focusGroup()

/**
 * The entry's own lifecycle, capped by its place in the stack ([StackScene.capOf]). The cap is
 * applied after the frame that changes it, so collectors restart outside the frame that composes a
 * transition.
 */
@Composable
private fun rememberEntryLifecycle(scene: StackScene, key: Any): LifecycleOwner {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { CappedLifecycle() }
    DisposableEffect(parent, owner) {
        val observer = LifecycleEventObserver { source, _ -> owner.follow(source.lifecycle.currentState) }
        parent.lifecycle.addObserver(observer)
        onDispose {
            parent.lifecycle.removeObserver(observer)
            owner.end()
        }
    }
    LaunchedEffect(scene, key, owner) {
        snapshotFlow { scene.capOf(key) }.collect { cap ->
            owner.cap = cap
            owner.follow(parent.lifecycle.currentState)
        }
    }
    return owner
}
