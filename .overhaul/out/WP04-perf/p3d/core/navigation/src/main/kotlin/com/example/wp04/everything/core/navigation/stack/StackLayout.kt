package com.example.wp04.everything.core.navigation.stack

import androidx.compose.foundation.background
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
import com.example.wp04.everything.core.common.navigation.AppNavKey
import com.example.wp04.everything.core.designsystem.R
import com.example.wp04.everything.core.designsystem.component.feedback.AppEmptyState
import com.example.wp04.everything.core.designsystem.foundation.layout.LocalPaneContext
import com.example.wp04.everything.core.designsystem.foundation.layout.LocalTabVisible
import com.example.wp04.everything.core.designsystem.foundation.layout.PaneContext
import com.example.wp04.everything.core.designsystem.foundation.layout.PaneRole
import com.example.wp04.everything.core.designsystem.foundation.layout.TabVisibility
import com.example.wp04.everything.core.designsystem.icon.AppIcons
import com.example.wp04.everything.core.designsystem.theme.AppTheme
import com.example.wp04.everything.core.navigation.NavPane
import com.example.wp04.everything.core.navigation.shell.tabs.CappedLifecycle

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
    // move, since the vote passes down to every layer of every screen in the stack, and read from
    // what changes on a transition's first and last frames anyway, so it adds no composition.
    val sliding = plan.slide != Slide.None && scene.settled !== plan
    val rate = if (sliding || scene.card.active) FrameRateCategory.High else FrameRateCategory.Default
    val panes = remember(plan, shown) { paneContexts(plan, shown) }
    Layout(
        modifier = modifier.preferredFrameRate(rate),
        content = {
            shown.forEach { entry ->
                key(entry.contentKey) {
                    EntrySlot(
                        entry = entry,
                        scene = scene,
                        plan = plan,
                        onTop = entry.contentKey in plan.top.shown,
                        pane = panes[entry.contentKey] ?: SinglePane,
                        tab = tab,
                        isLight = isLight,
                    )
                }
            }
            plan.emptyDetails.forEach { empty ->
                key(empty) { EmptyDetailSlot(empty, scene, plan, isLight) }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        scene.width = width.toFloat()
        scene.height = height.toFloat()
        scene.rtl = layoutDirection == LayoutDirection.Rtl
        if (scene.density.density != density || scene.density.fontScale != fontScale) scene.density = Density(density, fontScale)
        val pane = listPaneWidth.roundToPx()
        val keys = shown.map { it.contentKey } + plan.emptyDetails
        val bounds = keys.map { plan.boundsOf(it, width, pane) }
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(bounds[index].width, height))
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val x = bounds[index].left
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
    pane: PaneContext,
    tab: TabVisibility,
    isLight: Boolean,
) {
    val key = entry.contentKey
    val lifecycle = rememberEntryLifecycle(scene, key)
    val visibility = remember(scene, key, tab) { TabVisibility { tab.isVisible && key in scene.top } }
    val divided = plan.followsAnotherPane(key)
    val divider = AppTheme.colors.divider
    val draw = remember(scene, key, isLight, divided, divider) {
        Modifier
            .graphicsLayer { with(scene) { pose(key) } }
            .drawWithContent {
                with(scene) { drawOver(key, isLight) }
                if (divided) drawPaneDivider(divider)
            }
    }
    CompositionLocalProvider(
        LocalLifecycleOwner provides lifecycle,
        LocalTabVisible provides visibility,
        LocalPaneContext provides pane,
    ) {
        Box(draw.then(if (onTop) Modifier else Covered)) { entry.Content() }
    }
}

@Composable
private fun EmptyDetailSlot(key: EmptyDetail, scene: StackScene, plan: StackPlan, isLight: Boolean) {
    val divider = AppTheme.colors.divider
    val background = AppTheme.colors.background
    val divided = plan.followsAnotherPane(key)
    // Opaque, as every entry is (EntryFrame): the pane slides with its page over another one.
    val draw = remember(scene, key, isLight, divided, divider, background) {
        Modifier
            .graphicsLayer { with(scene) { pose(key) } }
            .drawWithContent {
                with(scene) { drawOver(key, isLight) }
                if (divided) drawPaneDivider(divider)
            }
            .background(background)
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
 * Each shown entry's pane: its role, whether another pane of its page shares the window, how many
 * details deep it is, and, for a list, the key of the detail open beside it.
 */
private fun paneContexts(plan: StackPlan, shown: List<NavEntry<AppNavKey>>): Map<Any, PaneContext> {
    val entries = shown.associateBy { it.contentKey }
    val contexts = HashMap<Any, PaneContext>(shown.size)
    for (page in plan.pages) {
        val beside = !page.single
        val selected = page.shown.entries.firstOrNull { it.value == Slot.Detail }?.key?.let { entries[it]?.navKey }
        var details = 0
        for (key in page.members) {
            val entry = entries[key] ?: continue
            val role = entry.pane.role
            if (role == PaneRole.Detail) details++
            contexts[key] = PaneContext(
                role = role,
                beside = beside && key in page.shown,
                detailDepth = if (role == PaneRole.Detail) details else 0,
                selectedKey = if (role == PaneRole.List) selected else null,
            )
        }
    }
    return contexts
}

private val NavPane.role: PaneRole
    get() = when (this) {
        NavPane.Single -> PaneRole.Single
        NavPane.List -> PaneRole.List
        NavPane.Detail -> PaneRole.Detail
        NavPane.Extra -> PaneRole.Extra
        NavPane.Main -> PaneRole.Main
        NavPane.Supporting -> PaneRole.Supporting
    }

private val SinglePane = PaneContext()

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
