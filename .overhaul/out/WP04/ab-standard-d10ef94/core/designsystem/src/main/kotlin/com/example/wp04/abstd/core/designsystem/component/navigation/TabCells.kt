package com.example.wp04.abstd.core.designsystem.component.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import com.example.wp04.abstd.core.designsystem.component.feedback.AppBadgedBox
import com.example.wp04.abstd.core.designsystem.theme.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * The tabs are drawn twice from one layout: the base layer in the resting colours, outlined
 * glyphs, everywhere outside the selection pill, and the selected layer, filled glyphs in the
 * selected colour, only inside it. Per frame only the clip moves, so a tab the pill is half over
 * is half selected, with no colour lerped across the whole tab and nothing recomposed.
 */
internal enum class TabLayer { Base, Selected }

/** Where the pill rests in each slot, along the bar's axis from its start edge, in px. */
@Immutable
internal class PillSlots(
    private val starts: FloatArray,
    private val ends: FloatArray,
    val crossStart: Float,
    val crossEnd: Float,
) {
    val count: Int get() = starts.size

    fun startAt(position: Float): Float = interpolate(starts, position)

    fun endAt(position: Float): Float = interpolate(ends, position)

    fun lengthAt(position: Float): Float = endAt(position) - startAt(position)

    private fun interpolate(values: FloatArray, position: Float): Float {
        if (values.isEmpty()) return 0f
        val p = position.coerceIn(0f, values.lastIndex.toFloat())
        val low = floor(p).toInt()
        val high = ceil(p).toInt()
        return lerp(values[low], values[high], p - low)
    }

    override fun equals(other: Any?): Boolean = other is PillSlots &&
        starts.contentEquals(other.starts) && ends.contentEquals(other.ends) &&
        crossStart == other.crossStart && crossEnd == other.crossEnd

    override fun hashCode(): Int = 31 * starts.contentHashCode() + ends.contentHashCode()

    companion object {
        val Empty = PillSlots(FloatArray(0), FloatArray(0), 0f, 0f)
    }
}

/**
 * The selection pill's two edges, in slot units. The edge facing the new tab runs ahead on a
 * quicker spring with a little give and the trailing one follows, so the pill stretches toward its
 * target and contracts as it lands, instead of sliding over as a rigid block. Read in draw only.
 */
@Stable
internal class IndicatorEdges(selected: Int) {
    val start = Animatable(selected.toFloat())
    val end = Animatable(selected.toFloat())

    /** Whether the pill last moved toward the end side; the trailing edge is the one clamped. */
    var forward = true
        private set

    suspend fun moveTo(target: Int) = coroutineScope {
        val to = target.toFloat()
        forward = to > start.value
        val lead = if (forward) end else start
        val trail = if (forward) start else end
        launch { lead.animateTo(to, LeadSpring) }
        trail.animateTo(to, TrailSpring)
    }

    /** The pill's drawn span in slot units, never wider than [MAX_STRETCH] past one slot. */
    inline fun span(block: (from: Float, to: Float) -> Unit) {
        var from = min(start.value, end.value)
        var to = max(start.value, end.value)
        if (to - from > MAX_STRETCH) {
            if (forward) from = to - MAX_STRETCH else to = from + MAX_STRETCH
        }
        block(from, to)
    }

    companion object {
        const val MAX_STRETCH = 0.5f

        /** How much the pill thins per slot of stretch, so it reads as a gel rather than a bar. */
        const val SQUASH = 0.06f
    }
}

/**
 * Each tab's glyph dips and springs back when the user selects it, as an SF Symbol bounce does:
 * it reads as the glyph being pressed, on a bar whose items have no press overlay of their own,
 * and it never grows the glyph past the pill.
 */
@Stable
internal class TabBounce(count: Int) {
    private val scales = List(count) { Animatable(1f) }

    fun scale(index: Int): Float = scales.getOrNull(index)?.value ?: 1f

    suspend fun play(index: Int) {
        val scale = scales.getOrNull(index) ?: return
        scale.animateTo(BOUNCE_DOWN, BounceDown)
        scale.animateTo(1f, BounceUp)
    }

    private companion object {
        const val BOUNCE_DOWN = 0.84f
        val BounceDown: SpringSpec<Float> = spring(dampingRatio = 1f, stiffness = 1500f, visibilityThreshold = 0.001f)
        val BounceUp: SpringSpec<Float> = spring(dampingRatio = 0.45f, stiffness = 500f, visibilityThreshold = 0.001f)
    }
}

/** The tab cells of one [TabLayer]; [onTap] only on the base layer, which carries the semantics. */
@Composable
internal fun TabCells(
    items: List<TabItem>,
    selected: Int,
    layer: TabLayer,
    labels: TabLabels,
    look: BarLook,
    bounce: TabBounce,
    measure: TabMeasure,
    onTap: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val color = if (layer == TabLayer.Base) look.content else look.selectedContent
    val filter = remember(color) { ColorFilter.tint(color) }
    Layout(
        modifier = modifier,
        content = {
            items.forEachIndexed { index, item ->
                val showLabel = labels == TabLabels.Always || (labels == TabLabels.Selected && index == selected)
                val cell = if (onTap != null) {
                    Modifier
                        .testTag("tab_$index")
                        .selectable(
                            selected = index == selected,
                            onClick = { onTap(index) },
                            role = Role.Tab,
                            interactionSource = null,
                            indication = null,
                        )
                        .then(if (showLabel) Modifier else Modifier.semantics { contentDescription = item.label })
                } else {
                    Modifier.clearAndSetSemantics {}
                }
                TabCell(
                    item = item,
                    glyph = if (layer == TabLayer.Selected) item.selectedIcon else item.icon,
                    color = color,
                    filter = filter,
                    showLabel = showLabel,
                    uppercase = look.uppercaseLabels,
                    scale = { bounce.scale(index) },
                    modifier = cell,
                )
            }
        },
        measurePolicy = measure.policy(layer),
    )
}

@Composable
private fun TabCell(
    item: TabItem,
    glyph: ImageVector,
    color: Color,
    filter: ColorFilter,
    showLabel: Boolean,
    uppercase: Boolean,
    scale: () -> Float,
    modifier: Modifier,
) {
    Layout(
        modifier = modifier,
        content = {
            AppBadgedBox(count = item.badgeCount) {
                Image(
                    painter = rememberVectorPainter(glyph),
                    contentDescription = null,
                    colorFilter = filter,
                    modifier = Modifier
                        .size(IconSize)
                        .graphicsLayer {
                            val s = scale()
                            scaleX = s
                            scaleY = s
                        },
                )
            }
            if (showLabel) {
                // The label grows with the font scale, but only so far: past 1.3× it no longer fits
                // the bar, and the glyph still says which tab it is.
                val density = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(MAX_LABEL_SCALE)),
                ) {
                    BasicText(
                        text = if (uppercase) item.label.uppercase() else item.label,
                        style = AppTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
                        color = { color },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val icon = measurables[0].measure(Constraints())
        // A label never reaches past the pill: it keeps the pill's gap to the slot edge and the
        // pill's own padding inside it.
        val labelRoom = (width - 2 * (SlotGap + LabelPaddingMin).roundToPx()).coerceAtLeast(0)
        val label = measurables.getOrNull(1)?.measure(Constraints(maxWidth = labelRoom))
        val content = icon.height + (label?.let { LabelGap.roundToPx() + it.height } ?: 0)
        val top = (height - content) / 2
        val contentStart = (width - (label?.width ?: icon.width).coerceAtLeast(icon.width)) / 2
        layout(width, height, mapOf(ContentStart to contentStart)) {
            icon.placeRelative((width - icon.width) / 2, top)
            label?.placeRelative((width - label.width) / 2, top + icon.height + LabelGap.roundToPx())
        }
    }
}

/**
 * How the tab cells are measured and placed along the bar or the rail, and where the pill rests in
 * each slot. The base layer writes [slots]; the selected layer is placed identically.
 */
@Stable
internal class TabMeasure(
    private val vertical: Boolean,
    private val state: AppTabBarState?,
    private val selected: () -> Int,
    val slots: MutableState<PillSlots>,
) {
    /** Px of rail height the cells may use before they scroll; written by the rail's viewport. */
    var railViewport by mutableIntStateOf(Constraints.Infinity)

    private val basePolicy = if (vertical) railPolicy(writesSlots = true) else barPolicy(writesSlots = true)
    private val selectedPolicy = if (vertical) railPolicy(writesSlots = false) else barPolicy(writesSlots = false)

    fun policy(layer: TabLayer): MeasurePolicy = if (layer == TabLayer.Base) basePolicy else selectedPolicy

    private fun barPolicy(writesSlots: Boolean) = MeasurePolicy { measurables, constraints ->
        val width = constraints.maxWidth
        val height = BarHeight.roundToPx()
        val count = measurables.size.coerceAtLeast(1)
        val slot = width / count
        val placeables = measurables.map { it.measure(Constraints.fixed(slot, height)) }
        if (writesSlots) {
            val starts = FloatArray(placeables.size)
            val ends = FloatArray(placeables.size)
            placeables.forEachIndexed { index, placeable ->
                val content = (slot - 2 * placeable.contentStart()).toFloat()
                val pill = (content + 2 * LabelPadding.toPx())
                    .coerceIn(MinPillWidth.toPx(), MaxPillWidth.toPx())
                    .coerceAtLeast(content + 2 * LabelPaddingMin.toPx())
                    .coerceAtMost(slot - 2 * SlotGap.toPx())
                starts[index] = index * slot + (slot - pill) / 2f
                ends[index] = starts[index] + pill
            }
            val inset = (height - PillHeight.toPx()) / 2f
            publish(PillSlots(starts, ends, inset, height - inset))
        }
        layout(width, height) {
            val collapse = state?.collapse ?: 0f
            val folded = MinimizedWidth.roundToPx()
            val current = selected()
            placeables.forEachIndexed { index, placeable ->
                if (index == current) {
                    // Folding, the selected tab moves to the middle of the narrowing capsule.
                    placeable.placeRelative(lerp(index * slot.toFloat(), (folded - slot) / 2f, collapse).toInt(), 0)
                } else {
                    placeable.placeRelativeWithLayer(index * slot, 0) { alpha = (1f - collapse * 2f).coerceIn(0f, 1f) }
                }
            }
        }
    }

    private fun railPolicy(writesSlots: Boolean) = MeasurePolicy { measurables, constraints ->
        val width = constraints.maxWidth
        val count = measurables.size.coerceAtLeast(1)
        // Items shrink toward their minimum before the rail scrolls, so a short window keeps every
        // tab in view as long as it can.
        val fit = if (railViewport == Constraints.Infinity) Int.MAX_VALUE else railViewport / count
        val item = fit.coerceIn(RailItemMinHeight.roundToPx(), RailItemHeight.roundToPx())
        val placeables = measurables.map { it.measure(Constraints.fixed(width, item)) }
        if (writesSlots) {
            val inset = RailPillInset.toPx()
            val starts = FloatArray(placeables.size) { it * item + inset }
            val ends = FloatArray(placeables.size) { (it + 1) * item - inset }
            val cross = (width - RailPillWidth.toPx()) / 2f
            publish(PillSlots(starts, ends, cross, width - cross))
        }
        layout(width, item * placeables.size) {
            placeables.forEachIndexed { index, placeable -> placeable.placeRelative(0, index * item) }
        }
    }

    private fun publish(next: PillSlots) {
        if (slots.value != next) slots.value = next
    }
}

/**
 * Fills [path] with the pill as [layer] clips to it: the slot span [from]…[to] of [slots] in a
 * row of [width] by [height] px, folded toward the middle by [collapse], mirrored for RTL.
 */
internal fun Path.setPill(
    slots: PillSlots,
    from: Float,
    to: Float,
    width: Float,
    height: Float,
    vertical: Boolean,
    rtl: Boolean,
    collapse: Float,
    minimizedWidth: Float,
    rule: Boolean,
) {
    rewind()
    if (slots.count == 0) return
    var start = slots.startAt(from)
    var end = slots.endAt(to)
    var crossStart = slots.crossStart
    var crossEnd = slots.crossEnd
    val rest = slots.lengthAt((from + to) / 2f)
    if (rest > 0f) {
        val stretch = (end - start) / rest
        val thin = (crossEnd - crossStart) * IndicatorEdges.SQUASH * (stretch - 1f).coerceAtLeast(0f) / 2f
        crossStart += thin
        crossEnd -= thin
    }
    if (collapse > 0f) {
        // Folded, the bar shows only its first [minimizedWidth] px from the start edge, and the
        // pill keeps the same inset on every side of that capsule.
        val foldedWidth = (minimizedWidth - 2 * slots.crossStart).coerceAtLeast(0f)
        val foldedStart = (minimizedWidth - foldedWidth) / 2f
        start = lerp(start, foldedStart, collapse)
        end = lerp(end, foldedStart + foldedWidth, collapse)
    }
    if (rule) {
        crossStart = 0f
        crossEnd = height
    }
    if (vertical) {
        addCapsule(crossStart, start, crossEnd, end, round = !rule)
    } else if (rtl) {
        addCapsule(width - end, crossStart, width - start, crossEnd, round = !rule)
    } else {
        addCapsule(start, crossStart, end, crossEnd, round = !rule)
    }
}

/**
 * A capsule built from lines and cubic quarter arcs, so drawing a moving pill allocates nothing
 * per frame (a RoundRect would be a new object each time).
 */
private fun Path.addCapsule(left: Float, top: Float, right: Float, bottom: Float, round: Boolean) {
    if (right <= left || bottom <= top) return
    val r = if (round) min(right - left, bottom - top) / 2f else 0f
    val k = r * KAPPA
    moveTo(left + r, top)
    lineTo(right - r, top)
    cubicTo(right - r + k, top, right, top + r - k, right, top + r)
    lineTo(right, bottom - r)
    cubicTo(right, bottom - r + k, right - r + k, bottom, right - r, bottom)
    lineTo(left + r, bottom)
    cubicTo(left + r - k, bottom, left, bottom - r + k, left, bottom - r)
    lineTo(left, top + r)
    cubicTo(left, top + r - k, left + r - k, top, left + r, top)
    close()
}

/** Where a cell's content (its label, or its glyph without one) starts, so the pill can fit it. */
private val ContentStart = VerticalAlignmentLine(::min)

private fun Placeable.contentStart(): Int = get(ContentStart).takeIf { it != AlignmentLine.Unspecified } ?: 0

/** The bezier handle that makes four cubics a circle. */
private const val KAPPA = 0.5522848f
private const val MAX_LABEL_SCALE = 1.3f

private val LeadSpring = spring(dampingRatio = 0.85f, stiffness = 815.6f, visibilityThreshold = 0.001f)
private val TrailSpring = spring(dampingRatio = 1f, stiffness = 385.5f, visibilityThreshold = 0.001f)
