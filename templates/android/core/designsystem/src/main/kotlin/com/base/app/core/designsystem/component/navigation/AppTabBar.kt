package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.base.app.core.designsystem.component.feedback.AppBadgedBox
import com.base.app.core.designsystem.foundation.HapticEffect
import com.base.app.core.designsystem.foundation.pressScale
import com.base.app.core.designsystem.foundation.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppColors
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** One tab: [selectedIcon] is the filled glyph, [icon] the outlined one. */
@Immutable
data class TabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val badgeCount: Int = 0,
)

/** The bar along the bottom edge, or stood on its side as a rail beside wide windows. */
enum class BarOrientation { Horizontal, Vertical }

/** Which tabs show their label. With [Never] the label is still read out by accessibility. */
enum class TabLabels { Always, Selected, Never }

/** Whether the floating bar shrinks to the selected tab while the content scrolls down. */
enum class BarMinimize { Never, OnScrollDown }

/**
 * The bar's minimize state. [nestedScrollConnection] goes around the content that drives it; the
 * shell installs it around the visible tab. [collapse] runs from 0 (full bar) to 1 (minimized) and
 * is read only in layout and draw. [settledHeight] changes only once the bar is at rest, so content
 * padding built from it never moves while the user is scrolling.
 */
@Stable
class AppTabBarState internal constructor(private val density: Density) {

    var collapse by mutableFloatStateOf(0f)
        private set

    /** The pixels the bar takes from the bottom of the window, written only when it is at rest. */
    var settledHeight by mutableIntStateOf(0)
        private set

    internal var behavior = BarMinimize.Never
    private var latestHeight = 0

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (behavior == BarMinimize.Never || consumed.y == 0f) return Offset.Zero
            // Content moving up (reading on) folds the bar away; any scroll back up opens it.
            val distance = with(density) { CollapseDistance.toPx() }
            collapse = (collapse - consumed.y / distance).coerceIn(0f, 1f)
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settleTo(if (collapse > 0.5f) 1f else 0f)
            return Velocity.Zero
        }
    }

    /** Opens a minimized bar, as tapping it does. */
    suspend fun expand() = settleTo(0f)

    // Run inside the scroll's fling, so the next drag cancels it rather than fighting it.
    private suspend fun settleTo(target: Float) {
        if (collapse != target) animate(collapse, target, animationSpec = SettleSpring) { value, _ -> collapse = value }
        settledHeight = latestHeight
    }

    internal fun onMeasured(height: Int) {
        latestHeight = height
        if (collapse == 0f || collapse == 1f) settledHeight = height
    }
}

@Composable
fun rememberAppTabBarState(): AppTabBarState {
    val density = LocalDensity.current
    return remember(density) { AppTabBarState(density) }
}

/** What a trailing action capsule or a floating toolbar puts its buttons through. */
interface BarActionScope {

    /** A round button with the bar's look; [tag] becomes its resource id for tests and benchmarks. */
    @Composable
    fun Action(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tag: String? = null)
}

/**
 * The app's tab bar: a floating capsule (or an edge-docked bar, as the design style says) along the
 * bottom of compact windows, and a rail beside wide ones. Switching tabs is instant; the selection
 * pill is the motion, its position and every colour it changes read in the draw phase, so a tab
 * switch recomposes nothing per frame. Tabs give no press overlay: the selection is the feedback.
 *
 * Tapping the selected tab calls [onReselect] (pop to root, or scroll to the top). [trailingAction]
 * is a separate capsule at the trailing end (search, compose); [accessory] sits above the bar (a
 * mini-player, an upload in progress) and is part of its height. [minimize] folds a floating bar to
 * its selected tab while [state]'s scroll connection sees the content scrolled down.
 */
@Composable
fun AppTabBar(
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
    trailingAction: (@Composable BarActionScope.() -> Unit)? = null,
    accessory: (@Composable () -> Unit)? = null,
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val onTap: (Int) -> Unit = { index ->
        when {
            state.collapse > 0f -> scope.launch { state.expand() }
            index == selected -> onReselect(index)
            else -> {
                haptics.perform(HapticEffect.Select)
                onSelect(index)
            }
        }
    }
    val indicator = animateFloatAsState(selected.toFloat(), SelectionSpring, label = "barIndicator")
    val look = barLook(treatment)
    if (orientation == BarOrientation.Vertical) {
        Rail(items, selected, indicator, labels, look, onTap, modifier)
        return
    }
    SideEffect { state.behavior = if (look.floating) minimize else BarMinimize.Never }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.onMeasured(it.height) }
            .then(
                if (look.floating) {
                    Modifier.navigationBarsPadding().padding(start = BarMargin, end = BarMargin, bottom = BarLift)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (accessory != null) {
            Box(Modifier.widthIn(max = BarMaxWidth).fillMaxWidth().collapsing(state, CollapseMode.Vanish)) {
                BarSurface(look, Modifier.fillMaxWidth().padding(bottom = AccessoryGap)) { accessory() }
            }
        }
        if (look.floating) {
            Box(Modifier.widthIn(max = BarMaxWidth).fillMaxWidth()) {
                BarSurface(
                    look = look,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = if (trailingAction != null) BarHeight + AccessoryGap else 0.dp),
                ) {
                    TabRow(items, selected, indicator, labels, look, state, onTap)
                }
                if (trailingAction != null) {
                    BarSurface(look, Modifier.align(Alignment.CenterEnd).size(BarHeight)) {
                        BarActions(look).trailingAction()
                    }
                }
            }
        } else {
            BarSurface(look, Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { TabRow(items, selected, indicator, labels, look, state, onTap) }
                    if (trailingAction != null) BarActions(look).trailingAction()
                }
            }
        }
    }
}

/**
 * A capsule of actions with the tab bar's look, for screens that need actions rather than tabs: an
 * editor, a photo viewer. It shares the bar's surface and shadow.
 */
@Composable
fun AppFloatingToolbar(
    modifier: Modifier = Modifier,
    treatment: BarTreatment = AppTheme.style.bar,
    actions: @Composable BarActionScope.() -> Unit,
) {
    val look = barLook(treatment)
    BarSurface(look = look, modifier = modifier.navigationBarsPadding().padding(bottom = BarLift)) {
        Row(
            modifier = Modifier.height(BarHeight).padding(horizontal = ItemInset),
            horizontalArrangement = Arrangement.spacedBy(ItemInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarActions(look).actions()
        }
    }
}

object AppTabBarDefaults {

    /** The floating bar's own height, without its margins or the navigation bar. */
    val Height: Dp = BarHeight

    /** The rail's width, cutout padding aside. */
    val RailWidth: Dp = 88.dp
}

/** What a treatment decides: whether the bar floats as a capsule, and its colours. */
@Immutable
private class BarLook(
    val floating: Boolean,
    val surface: Color,
    val border: Color,
    val shape: Shape,
    val indicator: Color,
    val selectedContent: Color,
    val content: Color,
)

@Composable
private fun barLook(treatment: BarTreatment): BarLook {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.pill
    return remember(treatment, colors, shape) { barLookFor(treatment, colors, shape) }
}

private fun barLookFor(treatment: BarTreatment, colors: AppColors, shape: Shape): BarLook = BarLook(
    floating = treatment == BarTreatment.Floating,
    surface = colors.surface,
    border = colors.divider,
    shape = shape,
    indicator = colors.accent,
    selectedContent = colors.onAccent,
    content = colors.contentSecondary,
)

@Composable
private fun BarSurface(look: BarLook, modifier: Modifier, content: @Composable () -> Unit) {
    val surface = if (look.floating) {
        modifier
            .dropShadow(look.shape, BarShadow)
            .clip(look.shape)
            .background(look.surface)
            .border(AppTheme.sizes.borderWidth, look.border, look.shape)
    } else {
        modifier
            .background(look.surface)
            .drawBehind { drawRect(look.border, size = Size(size.width, 1.dp.toPx())) }
            .navigationBarsPadding()
    }
    Box(surface, contentAlignment = Alignment.Center) { content() }
}

private enum class CollapseMode { Shrink, Vanish }

/**
 * Folds the bar with [AppTabBarState.collapse], in layout only: the tab row narrows to the selected
 * tab and the accessory closes. Reading the fraction here re-lays the bar out on each frame of a
 * fold and never recomposes it.
 */
private fun Modifier.collapsing(state: AppTabBarState, mode: CollapseMode): Modifier =
    layout { measurable, constraints ->
        val collapse = state.collapse
        val placeable = measurable.measure(constraints)
        if (collapse == 0f) return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        when (mode) {
            CollapseMode.Shrink -> {
                val width = lerp(placeable.width.toFloat(), MinimizedWidth.toPx(), collapse).roundToInt()
                layout(width.coerceAtMost(placeable.width), placeable.height) { placeable.place(0, 0) }
            }
            CollapseMode.Vanish -> {
                val height = (placeable.height * (1f - collapse)).roundToInt()
                layout(placeable.width, height) {
                    placeable.placeWithLayer(0, height - placeable.height) { alpha = 1f - collapse }
                }
            }
        }
    }

@Composable
private fun TabRow(
    items: List<TabItem>,
    selected: Int,
    indicator: State<Float>,
    labels: TabLabels,
    look: BarLook,
    state: AppTabBarState,
    onTap: (Int) -> Unit,
) {
    val count = items.size.coerceAtLeast(1)
    Layout(
        modifier = Modifier
            .collapsing(state, CollapseMode.Shrink)
            .selectableGroup()
            .drawBehind {
                // The pill sits in the slot the indicator has reached; folded, it fills the capsule.
                val slot = size.width / count
                val folded = state.collapse
                val inset = ItemInset.toPx()
                val x = lerp(indicator.value * slot, 0f, folded)
                val width = lerp(slot, MinimizedWidth.toPx(), folded) - inset * 2
                drawRoundRect(
                    color = look.indicator,
                    topLeft = Offset(x + inset, inset),
                    size = Size(width.coerceAtLeast(0f), size.height - inset * 2),
                    cornerRadius = CornerRadius(size.height / 2),
                )
            },
        content = {
            items.forEachIndexed { index, item ->
                TabCell(
                    item = item,
                    index = index,
                    selected = index == selected,
                    indicator = indicator,
                    showLabel = labels == TabLabels.Always || (labels == TabLabels.Selected && index == selected),
                    look = look,
                    onClick = { onTap(index) },
                )
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = BarHeight.roundToPx()
        val slot = width / count
        val placeables = measurables.map { it.measure(Constraints.fixed(slot, height)) }
        layout(width, height) {
            val collapse = state.collapse
            val folded = MinimizedWidth.roundToPx()
            placeables.forEachIndexed { index, placeable ->
                if (index == selected) {
                    // Folding, the selected tab moves to the middle of the narrowing capsule.
                    placeable.place(lerp(index * slot.toFloat(), (folded - slot) / 2f, collapse).roundToInt(), 0)
                } else {
                    placeable.placeWithLayer(index * slot, 0) { alpha = (1f - collapse * 2f).coerceIn(0f, 1f) }
                }
            }
        }
    }
}

@Composable
private fun TabCell(
    item: TabItem,
    index: Int,
    selected: Boolean,
    indicator: State<Float>,
    showLabel: Boolean,
    look: BarLook,
    onClick: () -> Unit,
) {
    // How much of the pill covers this tab, drawn into the glyph and label colours each frame.
    val tint = remember(index, look, indicator) {
        { lerp(look.content, look.selectedContent, (1f - abs(indicator.value - index)).coerceIn(0f, 1f)) }
    }
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .testTag("tab_$index")
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = null,
            )
            .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = item.label }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppBadgedBox(count = item.badgeCount) {
            val painter = rememberVectorPainter(if (selected) item.selectedIcon else item.icon)
            Box(
                Modifier
                    .size(IconSize)
                    .drawBehind { with(painter) { draw(size, colorFilter = ColorFilter.tint(tint())) } },
            )
        }
        if (showLabel) {
            // The label grows with the font scale, but only so far: past 1.3× it no longer fits the
            // bar, and the glyph still says which tab it is.
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(MAX_LABEL_SCALE)),
            ) {
                BasicText(
                    text = item.label,
                    style = AppTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = LabelGap, start = ItemInset, end = ItemInset),
                )
            }
        }
    }
}

@Composable
private fun Rail(
    items: List<TabItem>,
    selected: Int,
    indicator: State<Float>,
    labels: TabLabels,
    look: BarLook,
    onTap: (Int) -> Unit,
    modifier: Modifier,
) {
    Row(modifier = modifier.fillMaxHeight().background(look.surface)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                // Insets first: a cutout on this side widens the rail instead of squeezing it.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
                .width(AppTabBarDefaults.RailWidth)
                .padding(vertical = AppTheme.spacing.lg)
                .selectableGroup()
                .drawBehind {
                    val itemHeight = RailItemHeight.toPx()
                    val inset = ItemInset.toPx()
                    drawRoundRect(
                        color = look.indicator,
                        topLeft = Offset(inset * 2, indicator.value * itemHeight + inset),
                        size = Size(size.width - inset * 4, itemHeight - inset * 2),
                        cornerRadius = CornerRadius(itemHeight / 2),
                    )
                },
        ) {
            items.forEachIndexed { index, item ->
                Box(Modifier.fillMaxWidth().height(RailItemHeight)) {
                    TabCell(
                        item = item,
                        index = index,
                        selected = index == selected,
                        indicator = indicator,
                        showLabel = labels != TabLabels.Never,
                        look = look,
                        onClick = { onTap(index) },
                    )
                }
            }
        }
        Box(Modifier.fillMaxHeight().width(1.dp).background(look.border))
    }
}

private class BarActions(private val look: BarLook) : BarActionScope {
    @Composable
    override fun Action(icon: ImageVector, contentDescription: String, onClick: () -> Unit, tag: String?) {
        val interactions = remember { MutableInteractionSource() }
        val painter = rememberVectorPainter(icon)
        Box(
            modifier = Modifier
                .size(ActionSize)
                .then(if (tag != null) Modifier.testTag(tag) else Modifier)
                .pressScale(interactions)
                .clickable(
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Button,
                    onClickLabel = contentDescription,
                    onClick = onClick,
                )
                .semantics { this.contentDescription = contentDescription },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(IconSize)
                    .drawBehind { with(painter) { draw(size, colorFilter = ColorFilter.tint(look.content)) } },
            )
        }
    }
}

private val BarHeight = 64.dp
private val BarMargin = 20.dp
private val BarLift = 12.dp
private val BarMaxWidth = 480.dp
private val AccessoryGap = 8.dp
private val MinimizedWidth = 72.dp
private val CollapseDistance = 48.dp
private val ItemInset = 6.dp
private val IconSize = 24.dp
private val ActionSize = 52.dp
private val LabelGap = 2.dp
private val RailItemHeight = 68.dp
private const val MAX_LABEL_SCALE = 1.3f
private val BarShadow = Shadow(radius = 16.dp, color = Color.Black, offset = DpOffset(0.dp, 4.dp), alpha = 0.10f)

/*
 * Springs in iOS terms, stiffness = (2π / response)² and damping = 1 − bounce: the selection pill at
 * response 0.30 s with a little bounce, the minimize settle at the same response with none.
 */
private val SelectionSpring = spring(dampingRatio = 0.91f, stiffness = 438.6f, visibilityThreshold = 0.001f)
private val SettleSpring = spring(dampingRatio = 1f, stiffness = 438.6f, visibilityThreshold = 0.001f)
