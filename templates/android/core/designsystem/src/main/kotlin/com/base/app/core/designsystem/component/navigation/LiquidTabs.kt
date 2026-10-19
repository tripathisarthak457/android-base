package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import com.base.app.core.designsystem.foundation.glass.GlassDefaults
import com.base.app.core.designsystem.foundation.glass.LiquidLensState
import com.base.app.core.designsystem.foundation.glass.LocalGlassQuality
import com.base.app.core.designsystem.foundation.glass.LocalGlassScene
import com.base.app.core.designsystem.foundation.glass.SceneBackdrop
import com.base.app.core.designsystem.foundation.glass.liquidLens
import com.base.app.core.designsystem.foundation.glass.pressGlow
import com.base.app.core.designsystem.foundation.glass.recordScene
import com.base.app.core.designsystem.foundation.glass.rememberGlassChrome
import com.base.app.core.designsystem.foundation.glass.rememberLiquidLensState
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.BarTreatment
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedCornerStyle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * The tab bar as liquid glass: [AppTabBar] with [BarTreatment.Glass]. The capsule is the scene
 * behind it, blurred, saturated and bent inward along its rim, under a light tint and a rim of
 * light ([rememberGlassChrome]); the accessory and the trailing capsule share the look. The
 * selection is a lens: a drawn indicator at rest, and, on the Live tier, a drop of glass that
 * lifts off the bar as it travels to a tapped tab, or under a finger, which can drag it across
 * the tabs and let it go with its speed. Through it the scene and the selected tab's tinted
 * glyph are refracted and magnified, with the colours split at its rim. A held bar brightens
 * under the finger and gives a little in the direction of the drag.
 *
 * What the Kyant catalog's `LiquidBottomTabs` shows, at its parameters, built so that a tap or a
 * drag re-renders only the lens: the bar's sample is recorded once and re-rendered only when the
 * scene changes, its effect chain is fixed, and every value that moves is read in a layer block
 * or in draw. The rail stands the same bar on its side; it sits beside the content rather than
 * over it, so its body is opaque and its lens refracts the tinted tabs over that body.
 */
@Composable
internal fun LiquidTabs(
    items: List<TabItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit,
    orientation: BarOrientation,
    labels: TabLabels,
    minimize: BarMinimize,
    state: AppTabBarState,
    trailingAction: (@Composable BarActionScope.() -> Unit)?,
    accessory: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val reduceMotion = AppTheme.reduceMotion
    val bounce = remember(items.size) { TabBounce(items.size) }
    val look = barLook(BarTreatment.Glass)
    val springs = AppTheme.springs
    val vertical = orientation == BarOrientation.Vertical
    // The lens exists on the Live tier only; a rail has no scene, but its lens can still refract
    // the tinted tabs over its own body.
    val scene = LocalGlassScene.current
    val lensOn = LocalGlassQuality.current.refracts && (scene != null || vertical)
    val lens = if (lensOn) {
        rememberLiquidLensState(
            selectedSlot = selected,
            slotCount = items.size,
            trackSpring = springs.interactive.unit,
            settleSpring = springs.thumb.unit,
            pressSpring = springs.press.unit,
        )
    } else {
        null
    }
    val edges = remember { IndicatorEdges(selected) }
    if (lens == null) LaunchedEffect(edges, selected) { edges.moveTo(selected) }
    val span: PillSpan = if (lens != null) remember(lens) { LensSpan(lens) } else edges

    val choose: (Int) -> Unit = { index ->
        if (index != selected) {
            haptics.perform(AppHaptic.Selection)
            if (!reduceMotion) scope.launch { bounce.play(index) }
            onSelect(index)
        }
    }
    val onTap: (Int) -> Unit = { index ->
        when {
            state.collapse > 0f -> scope.launch { state.expand() }
            index == selected -> onReselect(index)
            else -> choose(index)
        }
    }
    val tabs = Tabs(items, selected, labels, look, bounce, span, lens, onTap, choose, onCross = { haptics.perform(AppHaptic.Selection) })

    if (vertical) {
        LiquidRail(tabs, trailingAction, accessory, modifier)
        return
    }
    val chrome = rememberGlassChrome()
    SideEffect { state.behavior = minimize }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.onMeasured(it.height) }
            .navigationBarsPadding()
            .padding(start = BarMargin, end = BarMargin, bottom = BarLift),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (accessory != null) {
            Box(Modifier.widthIn(max = BarMaxWidth).fillMaxWidth().vanishing(state)) {
                Box(Modifier.fillMaxWidth().padding(bottom = AccessoryGap).then(chrome), contentAlignment = Alignment.Center) {
                    accessory()
                }
            }
        }
        Box(Modifier.widthIn(max = BarMaxWidth).fillMaxWidth()) {
            LiquidBar(
                tabs = tabs,
                state = state,
                chrome = chrome,
                scene = scene,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(end = if (trailingAction != null) BarHeight + AccessoryGap else 0.dp),
            )
            if (trailingAction != null) {
                Box(Modifier.align(Alignment.CenterEnd).size(BarHeight).then(chrome), contentAlignment = Alignment.Center) {
                    BarActions(look).trailingAction()
                }
            }
        }
    }
}

/** What both the bar and the rail draw their tabs from. */
@Stable
private class Tabs(
    val items: List<TabItem>,
    val selected: Int,
    val labels: TabLabels,
    val look: BarLook,
    val bounce: TabBounce,
    val span: PillSpan,
    val lens: LiquidLensState?,
    val onTap: (Int) -> Unit,
    val choose: (Int) -> Unit,
    val onCross: () -> Unit,
)

/**
 * The capsule with its tabs and, above it as a sibling so it can swell past the capsule's edge, the
 * lens. The group moves with the bar's give; the capsule alone swells a little under a press. Both
 * are layer properties read per frame: the capsule's sample is not re-recorded for them, so the
 * blurred scene stretches with the glass, as it would through a lens.
 */
@Composable
private fun LiquidBar(
    tabs: Tabs,
    state: AppTabBarState,
    chrome: Modifier,
    scene: SceneBackdrop?,
    modifier: Modifier,
) {
    val lens = tabs.lens
    val current = rememberUpdatedState(tabs.selected)
    val measure = remember(state) {
        TabMeasure(vertical = false, state, { current.value }, mutableStateOf(PillSlots.Empty), uniformPills = true, pillHeight = LensHeight)
    }
    val pill = remember { PillPaths() }
    val tabsScene = rememberTabsScene(lens)
    val give = if (lens != null) {
        Modifier.graphicsLayer {
            val pull = lens.pull
            translationX = if (pull == 0f) 0f else BarGive.toPx() * sign(pull) * easeOut(abs(pull) / lens.slotCount)
        }
    } else {
        Modifier
    }
    val swell = if (lens != null) {
        Modifier.graphicsLayer {
            val scale = 1f + BarSwell.toPx() / size.width * lens.pressure
            scaleX = scale
            scaleY = scale
        }
    } else {
        Modifier
    }
    Box(modifier.then(give)) {
        Box(Modifier.then(swell).then(chrome), contentAlignment = Alignment.Center) {
            LensTabRow(tabs, measure, pill, state, tabsScene, Modifier.shrinking(state))
        }
        if (lens != null && scene != null && tabsScene != null) {
            Lens(
                lens, scene, tabsScene, tabs.look, Orientation.Horizontal,
                body = GlassDefaults.chromeColors().body, modifier = Modifier.lensSlot(lens, measure),
            )
        }
    }
}

/**
 * The rail: the same tabs, stood on their side, on an opaque body (it is beside the content, not
 * over it). On the Live tier its lens refracts the tinted tabs over that body.
 */
@Composable
private fun LiquidRail(
    tabs: Tabs,
    header: (@Composable BarActionScope.() -> Unit)?,
    footer: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val lens = tabs.lens
    val tabsScene = rememberTabsScene(lens)
    val body = tabs.look.surface
    val backdrop = rememberCanvasBackdrop { drawRect(body) }
    TabRail(tabs.items, tabs.selected, tabs.span, tabs.labels, tabs.look, tabs.bounce, tabs.onTap, header, footer, modifier) { measure, pill, railLabels ->
        LensTabRow(tabs.copy(labels = railLabels), measure, pill, state = null, tabsScene, Modifier)
        if (lens != null && tabsScene != null) {
            Lens(lens, backdrop, tabsScene, tabs.look, Orientation.Vertical, body = body, modifier = Modifier.lensSlot(lens, measure))
        }
    }
}

private fun Tabs.copy(labels: TabLabels) = Tabs(items, selected, labels, look, bounce, span, lens, onTap, choose, onCross)

/** The layer the tinted tabs are recorded into for the lens; none without a lens. */
@Composable
private fun rememberTabsScene(lens: LiquidLensState?): SceneBackdrop? {
    if (lens == null) return null
    val layer = rememberGraphicsLayer()
    return remember(layer) { SceneBackdrop(layer) { Color.Transparent } }
}

/**
 * The two cell layers under the lens: outlined glyphs outside the pill, tinted ones inside it. On
 * the Live tier the tinted layer is also recorded, unclipped, into [tabsScene], which the lens
 * samples with the scene: the tabs reach the lens as part of what it refracts, with no second
 * composition. While a finger is down the row brightens under it, inside the capsule's clip.
 */
@Composable
private fun LensTabRow(
    tabs: Tabs,
    measure: TabMeasure,
    pill: PillPaths,
    state: AppTabBarState?,
    tabsScene: SceneBackdrop?,
    modifier: Modifier,
) {
    val lens = tabs.lens
    val vertical = state == null
    val collapse: () -> Float = if (state != null) ({ state.collapse }) else ({ 0f })
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(
        modifier
            .focusRestorer()
            .focusGroup()
            .selectableGroup()
            .then(
                if (lens != null) {
                    Modifier
                        .liquidDrag(lens, measure, state, vertical, rtl, tabs.choose, tabs.onCross)
                        .pressGlow({ lens.pressure }, { glowAt(lens, measure, vertical, rtl) })
                } else {
                    Modifier
                },
            )
            .drawBehind { drawIndicator(pill.fill, measure, tabs.span, tabs.look, vertical, collapse = collapse()) },
    ) {
        TabCells(
            tabs.items, tabs.selected, TabLayer.Base, tabs.labels, tabs.look, tabs.bounce, measure, tabs.onTap,
            Modifier.clipToPill(pill.outside, measure, tabs.span, tabs.look, vertical, outside = true, collapse = collapse),
        )
        TabCells(
            tabs.items, tabs.selected, TabLayer.Selected, tabs.labels, tabs.look, tabs.bounce, measure, null,
            Modifier
                .clipToPill(pill.inside, measure, tabs.span, tabs.look, vertical, outside = false, collapse = collapse)
                .then(if (tabsScene != null) Modifier.recordScene(tabsScene) else Modifier),
        )
    }
}

/**
 * The lens element: one node, attached for the bar's lifetime at the pill's resting size.
 *
 * @param body what lies under the resting indicator: the chrome's tint, or the rail's opaque
 *   surface. At the hand-over the lens must look like the indicator it leaves behind, which is
 *   the indicator's colour over that.
 */
@Composable
private fun Lens(
    lens: LiquidLensState,
    scene: Backdrop,
    tabsScene: SceneBackdrop,
    look: BarLook,
    orientation: Orientation,
    body: Color,
    modifier: Modifier,
) {
    val combined = rememberCombinedBackdrop(scene, tabsScene)
    val surface = look.indicator.compositeOver(body)
    val colour = remember(surface) { ColorProducer { surface } }
    val shadow = GlassDefaults.lensShadow()
    val glass = remember(combined, lens, orientation, colour, shadow) {
        Modifier.liquidLens(combined, LensShape, lens, orientation, colour, shadow)
    }
    Box(modifier.then(glass))
}

/**
 * Sizes the lens to a pill (every slot's pill is the same size) and places it over the slot the
 * lens is at. The position is read in placement only, so a lens in flight is placed again each
 * frame and measured never.
 */
private fun Modifier.lensSlot(lens: LiquidLensState, measure: TabMeasure): Modifier = layout { measurable, constraints ->
    val slots = measure.slots.value
    val along = if (slots.count == 0) 0 else slots.lengthAt(0f).roundToInt()
    val across = (slots.crossEnd - slots.crossStart).roundToInt().coerceAtLeast(0)
    val vertical = measure.isVertical
    val width = if (vertical) across else along
    val height = if (vertical) along else across
    val placeable = measurable.measure(Constraints.fixed(width.coerceAtLeast(0), height.coerceAtLeast(0)))
    layout(constraints.maxWidth, constraints.maxHeight) {
        val start = if (slots.count == 0) 0f else slots.startAt(lens.position)
        if (vertical) {
            placeable.place(slots.crossStart.roundToInt(), start.roundToInt())
        } else {
            // Mirrored by hand: placeRelative would mirror the lens's own layer as well.
            val x = if (layoutDirection == LayoutDirection.Rtl) constraints.maxWidth - start - along else start
            placeable.place(x.roundToInt(), slots.crossStart.roundToInt())
        }
    }
}

/** The pill drawn where the lens is, so the lens covers it exactly when it appears or hands over. */
@Stable
private class LensSpan(private val lens: LiquidLensState) : PillSpan {
    override val from: Float get() = lens.position
    override val to: Float get() = lens.position
}

/** Where the glow pools on the row: under the lens, which trails the finger, as the catalog's does. */
private fun glowAt(lens: LiquidLensState, measure: TabMeasure, vertical: Boolean, rtl: Boolean): Offset {
    val slots = measure.slots.value
    if (slots.count == 0) return Offset.Zero
    val at = lens.position
    val along = (slots.startAt(at) + slots.endAt(at)) / 2f
    val across = (slots.crossStart + slots.crossEnd) / 2f
    return if (vertical) Offset(across, along) else Offset(if (rtl) measure.lengthPx - along else along, across)
}

/**
 * The drag that carries the lens: a finger held for [HoldMillis], or moved past the touch slop,
 * presses the lens where it is and takes it along; letting go selects where it lands, with its
 * speed. Each tab the finger crosses calls [onCross]. The handler watches the initial pass, so a
 * lifted finger after a hold is consumed before the cell under it sees a tap; a plain tap passes
 * through to the cell. A folded bar only expands.
 */
private fun Modifier.liquidDrag(
    lens: LiquidLensState,
    measure: TabMeasure,
    state: AppTabBarState?,
    vertical: Boolean,
    rtl: Boolean,
    choose: (Int) -> Unit,
    onCross: () -> Unit,
): Modifier = pointerInput(lens, measure, state, vertical, rtl) {
    liquidDragGestures(lens, measure, state, vertical, rtl, choose, onCross)
}

private suspend fun PointerInputScope.liquidDragGestures(
    lens: LiquidLensState,
    measure: TabMeasure,
    state: AppTabBarState?,
    vertical: Boolean,
    rtl: Boolean,
    choose: (Int) -> Unit,
    onCross: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if ((state?.collapse ?: 0f) > 0f) return@awaitEachGesture
        val count = measure.slots.value.count
        if (count == 0) return@awaitEachGesture
        val slop = viewConfiguration.touchSlop
        val length = (if (vertical) size.height else size.width).toFloat()
        val slotPx = length / count
        fun slotOf(change: PointerInputChange): Float {
            val along = if (vertical) change.position.y else change.position.x
            val forward = if (!vertical && rtl) length - along else along
            return (forward / slotPx - 0.5f).fastCoerceIn(0f, (count - 1).toFloat())
        }
        var pressed = false
        var crossed = slotOf(down).roundToInt()
        var travelled = 0f
        while (true) {
            val event = withTimeoutOrNull(HoldMillis) { awaitPointerEvent(PointerEventPass.Initial) }
            if (event == null) {
                if (!pressed) {
                    pressed = true
                    lens.press(slotOf(down))
                }
                continue
            }
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (change.changedToUpIgnoreConsumed()) {
                if (pressed) {
                    change.consume()
                    choose(lens.release())
                }
                break
            }
            if (change.isConsumed) {
                if (pressed) lens.cancel()
                break
            }
            val delta = change.positionChange()
            val step = if (vertical) delta.y else delta.x
            if (!pressed) {
                travelled += step
                if (abs(travelled) < slop) continue
                pressed = true
                lens.press(slotOf(change))
            } else {
                lens.dragBy((if (!vertical && rtl) -step else step) / slotPx)
            }
            change.consume()
            val now = lens.touch.roundToInt()
            if (now != crossed) {
                crossed = now
                onCross()
            }
        }
    }
}

/** How far the bar gives at a full drag: the catalog's 4 dp. */
private val BarGive = 4.dp

/** How much the capsule grows under a press, as the catalog's 16 dp over its width. */
private val BarSwell = 16.dp

/** The lens is taller than the default pill, so pressed it reaches past the capsule's edge. */
private val LensHeight = 50.dp

private val LensShape = Capsule(RoundedCornerStyle.Circular)
private const val HoldMillis = 150L

private fun easeOut(fraction: Float): Float {
    val f = fraction.fastCoerceIn(0f, 1f)
    return 1f - (1f - f) * (1f - f)
}
