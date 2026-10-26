package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * One tab: [selectedIcon] is the filled glyph, [icon] the outlined one. [section] is the heading it
 * is grouped under in a sidebar.
 */
@Immutable
public data class TabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
    val section: String? = null,
)

/** The bar along the bottom edge, or stood on its side as a rail beside wide windows. */
public enum class BarOrientation { Horizontal, Vertical }

/** Which tabs show their label. With [Never] the label is still read out by accessibility. */
public enum class TabLabels { Always, Selected, Never }

/** Whether the floating bar shrinks to the selected tab while the content scrolls down. */
public enum class BarMinimize { Never, OnScrollDown }

/** What a trailing action capsule or a floating toolbar puts its buttons through. */
public interface BarActionScope {

    /** A round button with the bar's look; [tag] becomes its resource id for tests and benchmarks. */
    @Composable
    public fun Action(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tag: String? = null)
}

/**
 * The app's tab bar: a floating capsule (or an edge-docked bar, as the design style says) along the
 * bottom of compact windows, and a rail beside wide ones. Switching tabs is instant; the bar is
 * the motion. The selection pill's leading edge runs ahead of its trailing one, so it stretches
 * toward the new tab and settles into it, and the new tab's glyph dips and springs back. Every
 * value that moves is read in the layout or draw phase, so a switch recomposes nothing per frame,
 * and tabs give no press overlay: the selection is the feedback.
 *
 * Tapping the selected tab calls [onReselect] (pop to root, or scroll to the top). [trailingAction]
 * is a separate capsule at the trailing end (search, compose), or the head of the rail;
 * [accessory] sits above the bar (a mini-player, an upload in progress) and is part of its height,
 * or at the foot of the rail. [minimize] folds a floating bar to its selected tab while [state]'s
 * scroll connection sees the content read on.
 *
 * A docked bar takes iOS's scroll edge appearance: its surface and top rule show in proportion to
 * [scrolledUnder], 1 while content runs under the bar and 0 once the content rests on its end
 * above it, so a list at its end is not cut by a hard edge. It is read in draw only.
 */
@Composable
public fun AppTabBar(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    orientation: BarOrientation = BarOrientation.Horizontal,
    treatment: BarTreatment = AppTheme.style.bar,
    labels: TabLabels = TabLabels.Always,
    minimize: BarMinimize = BarMinimize.Never,
    state: AppTabBarState = rememberAppTabBarState(),
    scrolledUnder: () -> Float = { 1f },
    trailingAction: (@Composable BarActionScope.() -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val reduceMotion = AppTheme.reduceMotion
    val bounce = remember(items.size) { TabBounce(items.size) }
    val edges = remember { IndicatorEdges(selected) }
    LaunchedEffect(edges, selected) { edges.moveTo(selected) }
    // Read through state rather than captured: a tap handler that captured the selection would be
    // a new one on every switch, and every cell of the bar would recompose in the switch's frame to
    // take it.
    val current = rememberUpdatedState(selected)
    val onTap: (Int) -> Unit = { index ->
        when {
            state.collapse > 0f -> scope.launch { state.expand() }
            index == current.value -> onReselect(index)
            else -> {
                haptics.perform(AppHaptic.Selection)
                if (!reduceMotion) scope.launch { bounce.play(index) }
                onSelect(index)
            }
        }
    }
    val look = barLook(treatment)
    if (orientation == BarOrientation.Vertical) {
        TabRail(items, selected, edges, labels, look, bounce, onTap, trailingAction, accessory, modifier)
        return
    }
    SideEffect { state.behavior = if (look.floating) minimize else BarMinimize.Never }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.onMeasured(it.height) }
            .then(
                if (look.floating) {
                    Modifier.navigationBarsPadding().padding(start = AppTabBarDefaults.BarMargin, end = AppTabBarDefaults.BarMargin, bottom = AppTabBarDefaults.BarLift)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (accessory != null) {
            Box(Modifier.widthIn(max = AppTabBarDefaults.BarMaxWidth).fillMaxWidth().vanishing(state)) {
                BarSurface(look, Modifier.fillMaxWidth().padding(bottom = AppTabBarDefaults.AccessoryGap)) { accessory() }
            }
        }
        if (look.floating) {
            Box(Modifier.widthIn(max = AppTabBarDefaults.BarMaxWidth).fillMaxWidth()) {
                BarSurface(
                    look = look,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = if (trailingAction != null) AppTabBarDefaults.Height + AppTabBarDefaults.AccessoryGap else 0.dp),
                ) {
                    TabRow(items, selected, edges, labels, look, state, bounce, onTap)
                }
                if (trailingAction != null) {
                    BarSurface(look, Modifier.align(Alignment.CenterEnd).size(AppTabBarDefaults.Height)) {
                        BarActions(look).trailingAction()
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val shown = scrolledUnder()
                        if (shown > 0f) {
                            drawRect(look.surface, alpha = shown)
                            drawRect(look.border, size = Size(size.width, 1.dp.toPx()), alpha = shown)
                        }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = AppTabBarDefaults.DockedInset),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { TabRow(items, selected, edges, labels, look, state, bounce, onTap) }
                if (trailingAction != null) BarActions(look).trailingAction()
            }
        }
    }
}

/**
 * A capsule of actions with the tab bar's look, for screens that need actions rather than tabs: an
 * editor, a photo viewer. It shares the bar's surface and shadow.
 */
@Composable
public fun AppFloatingToolbar(
    modifier: Modifier = Modifier,
    treatment: BarTreatment = AppTheme.style.bar,
    actions: @Composable BarActionScope.() -> Unit,
) {
    val look = barLook(treatment)
    BarSurface(look = look, modifier = modifier.navigationBarsPadding().padding(bottom = AppTabBarDefaults.BarLift)) {
        Row(
            modifier = Modifier.height(AppTabBarDefaults.Height).padding(horizontal = AppTabBarDefaults.ToolbarInset),
            horizontalArrangement = Arrangement.spacedBy(AppTabBarDefaults.ToolbarInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarActions(look).actions()
        }
    }
}

/** The tab bar's and the rail's fixed measurements, and the surface they share with the sidebar. */
public object AppTabBarDefaults {

    /** The floating bar's own height, without its margins or the navigation bar. */
    public val Height: Dp = 64.dp

    /** The rail's width, cutout padding aside. */
    public val RailWidth: Dp = 88.dp

    internal val MinimizedWidth = 72.dp
    internal val IconSize = 24.dp
    internal val LabelGap = 2.dp

    /** The pill: 44 dp in the 64 dp bar, so it never crowds the capsule's edges. */
    internal val PillHeight = 44.dp
    internal val MinPillWidth = 52.dp
    internal val MaxPillWidth = 72.dp

    /** Between a pill and its slot's edge, so two neighbouring pills never touch. */
    internal val SlotGap = 4.dp

    /**
     * Inside the pill on each side of the label; it gives way to [LabelPaddingMin] in a narrow
     * slot.
     */
    internal val LabelPadding = 10.dp
    internal val LabelPaddingMin = 4.dp

    internal val RailItemHeight = 68.dp
    internal val RailItemMinHeight = 56.dp
    internal val RailPillWidth = 64.dp
    internal val RailPillInset = 4.dp
    internal val RuleHeight = 2.dp

    internal val BarMargin = 20.dp

    /**
     * Between a docked bar's outer tabs and the screen's edges. Labels may fill their slots at the
     * largest text size, and this keeps the outermost ones off the edge as they sit at the default.
     */
    internal val DockedInset = 8.dp
    internal val BarLift = 12.dp
    internal val BarMaxWidth = 480.dp
    internal val AccessoryGap = 8.dp
    internal val ToolbarInset = 6.dp
    internal val ActionSize = 52.dp
    internal val BarShadow = Shadow(radius = 16.dp, offset = DpOffset(0.dp, 4.dp), alpha = 0.10f)

    /** A floating bar's edge in dark theme, where its shadow would not show. */
    internal const val DarkOutlineAlpha = 0.07f

    /** The surface a docked bar, the rail and the sidebar are drawn on. */
    @Composable
    public fun surface(treatment: BarTreatment = AppTheme.style.bar): Color = barLook(treatment).surface
}

@Composable
internal fun BarSurface(look: BarLook, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val surface = if (look.floating) {
        modifier
            .then(if (look.shadow) Modifier.dropShadow(look.shape, AppTabBarDefaults.BarShadow) else Modifier)
            .clip(look.shape)
            .background(look.surface)
            .border(AppTheme.style.borderWidth, look.border, look.shape)
    } else {
        modifier
            .background(look.surface)
            .drawBehind { drawRect(look.border, size = Size(size.width, 1.dp.toPx())) }
    }
    Box(surface, contentAlignment = Alignment.Center) { content() }
}

/** The accessory closes with the minimize, in layout only: no recomposition per frame. */
private fun Modifier.vanishing(state: AppTabBarState): Modifier = layout { measurable, constraints ->
    val collapse = state.collapse
    val placeable = measurable.measure(constraints)
    if (collapse == 0f) return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    val height = (placeable.height * (1f - collapse)).roundToInt()
    layout(placeable.width, height) {
        placeable.placeWithLayer(0, height - placeable.height) { alpha = 1f - collapse }
    }
}

/**
 * The tab row narrows to [MinimizedWidth] with the minimize, in layout only. The row keeps its
 * full width inside, placed from its start edge, and the surface clips it.
 */
private fun Modifier.shrinking(state: AppTabBarState): Modifier = layout { measurable, constraints ->
    val collapse = state.collapse
    val placeable = measurable.measure(constraints)
    val width = lerp(placeable.width.toFloat(), AppTabBarDefaults.MinimizedWidth.toPx(), collapse).roundToInt()
    layout(width.coerceAtMost(placeable.width), placeable.height) { placeable.placeRelative(0, 0) }
}

@Composable
private fun TabRow(
    items: List<TabItem>,
    selected: Int,
    edges: IndicatorEdges,
    labels: TabLabels,
    look: BarLook,
    state: AppTabBarState,
    bounce: TabBounce,
    onTap: (Int) -> Unit,
) {
    val current = rememberUpdatedState(selected)
    val measure = remember(state) { TabMeasure(vertical = false, state, { current.value }, mutableStateOf(PillSlots.Empty)) }
    val pill = remember { PillPaths() }
    Box(
        Modifier
            .shrinking(state)
            .focusRestorer()
            .focusGroup()
            .selectableGroup()
            .drawBehind { drawIndicator(pill.fill, measure, edges, look, vertical = false, collapse = state.collapse) },
    ) {
        TabCells(
            items, selected, TabLayer.Base, labels, look, bounce, measure, onTap,
            Modifier.clipToPill(pill.outside, measure, edges, look, vertical = false, outside = true, collapse = { state.collapse }),
        )
        TabCells(
            items, selected, TabLayer.Selected, labels, look, bounce, measure, null,
            Modifier.clipToPill(pill.inside, measure, edges, look, vertical = false, outside = false, collapse = { state.collapse }),
        )
    }
}

/** One path per use, each rewritten in place every frame the pill moves. */
internal class PillPaths {
    val fill = Path()
    val outside = Path()
    val inside = Path()
}

internal fun DrawScope.drawIndicator(
    path: Path,
    measure: TabMeasure,
    edges: IndicatorEdges,
    look: BarLook,
    vertical: Boolean,
    collapse: Float,
) {
    edges.span { from, to ->
        path.setPill(
            measure.slots.value, from, to, size.width, size.height, vertical,
            rtl = layoutDirection == LayoutDirection.Rtl, collapse = collapse,
            minimizedWidth = AppTabBarDefaults.MinimizedWidth.toPx(), rule = look.rule,
        )
    }
    if (look.rule) {
        clipPath(path) { drawRect(look.indicator, size = Size(size.width, AppTabBarDefaults.RuleHeight.toPx())) }
    } else {
        drawPath(path, look.indicator)
    }
}

/** Draws a layer only outside the pill ([outside]) or only inside it. */
internal fun Modifier.clipToPill(
    path: Path,
    measure: TabMeasure,
    edges: IndicatorEdges,
    look: BarLook,
    vertical: Boolean,
    outside: Boolean,
    collapse: () -> Float,
): Modifier = drawWithContent {
    edges.span { from, to ->
        path.setPill(
            measure.slots.value, from, to, size.width, size.height, vertical,
            rtl = layoutDirection == LayoutDirection.Rtl, collapse = collapse(),
            minimizedWidth = AppTabBarDefaults.MinimizedWidth.toPx(), rule = look.rule,
        )
    }
    clipPath(path, if (outside) ClipOp.Difference else ClipOp.Intersect) { this@drawWithContent.drawContent() }
}

internal class BarActions(private val look: BarLook) : BarActionScope {
    @Composable
    override fun Action(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tag: String?) {
        val filter = remember(look) { ColorFilter.tint(look.content) }
        Box(
            modifier = Modifier
                .size(AppTabBarDefaults.ActionSize)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .pressable(
                    onClick = onClick,
                    feedback = PressFeedback.Scale,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Image(rememberVectorPainter(icon), contentDescription = null, colorFilter = filter, modifier = Modifier.size(AppTabBarDefaults.IconSize))
        }
    }
}

@AppComponentPreviews
@Composable
private fun AppTabBarPreview() {
    val items = listOf(
        TabItem(label = "Home", icon = AppIcons.Home),
        TabItem(label = "Search", icon = AppIcons.Search),
        TabItem(label = "Inbox", icon = AppIcons.Mail, badgeCount = 3),
        TabItem(label = "Settings", icon = AppIcons.Settings),
    )
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTabBar(items = items, selected = 0, onSelect = {}, onReselect = {})
        AppTabBar(items = items, selected = 2, onSelect = {}, onReselect = {}, labels = TabLabels.Selected)
        AppTabBar(
            items = items.take(3),
            selected = 1,
            onSelect = {},
            onReselect = {},
            trailingAction = { Action(icon = AppIcons.Plus, contentDescription = "New", onClick = {}) },
        )
        AppFloatingToolbar {
            Action(icon = AppIcons.Share, contentDescription = "Share", onClick = {})
            Action(icon = AppIcons.Heart, contentDescription = "Like", onClick = {})
            Action(icon = AppIcons.Trash, contentDescription = "Delete", onClick = {})
        }
    }
}
