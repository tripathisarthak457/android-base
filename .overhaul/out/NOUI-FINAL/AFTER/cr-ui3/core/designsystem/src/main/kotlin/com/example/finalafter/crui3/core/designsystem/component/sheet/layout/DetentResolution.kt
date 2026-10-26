package com.example.finalafter.crui3.core.designsystem.component.sheet.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetDetent
import com.example.finalafter.crui3.core.designsystem.component.sheet.SheetDetentContext
import com.example.finalafter.crui3.core.designsystem.foundation.adaptive.SizeClass
import com.example.finalafter.crui3.core.designsystem.foundation.adaptive.WindowHeight
import com.example.finalafter.crui3.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/**
 * Everything settled that a sheet's stops depend on, in pixels from the top of its bounds. None of
 * it changes during a drag or with the keyboard, so stops resolve in measure and only when one of
 * these changes.
 */
@Immutable
internal data class DetentInputs(
    /** H: the height of the sheet's bounds. */
    val boundsHeight: Float,
    /** Large's top: the safe top plus the gap, or a covered sheet's lifted top plus the peek. */
    val largeTop: Float,
    /** The line heights are measured up from: above the navigation bar, or the tab bar's top. */
    val restBottom: Float,
    /** FitContent's height: grabber band or header, content and footer, without the keyboard. */
    val contentHeight: Float,
    /** Peek's height: the grabber band, the header and the room to a floating edge; 0 without a header. */
    val headerHeight: Float,
    /** The placement class. */
    val sizeClass: SizeClass,
    /** The bottom of a horizontal hinge from the top of the bounds, or null. */
    val foldBottom: Float?,
    val density: Float,
    val fontScale: Float,
)

/**
 * Where each active detent puts the sheet along its travel, tallest first, and where the sheet goes
 * when it hides. Offsets grow toward hidden: a bottom sheet's top edge, or a side panel's distance
 * from its resting place. Two equal sets are the same stops, so anchors are written only when one
 * changed.
 */
@Immutable
internal class SheetStops(
    /** The active detents, tallest first. */
    val detents: List<SheetDetent>,
    /** Their offsets, ascending. */
    val offsets: FloatArray,
    val hidden: Float,
    val largeTop: Float,
    val restBottom: Float,
    /** The stop that counts as Large, or -1. */
    val largeIndex: Int,
    /** How far past the tallest stop the rubber band reaches: its asymptote. */
    val roomAbove: Float,
    /** The length of the bounds along the travel, H or W: dismissal speeds are measured in it. */
    val span: Float = hidden,
    /**
     * A form sheet or a modal side panel: one stop, whose detent is the one recorded for when the
     * sheet returns to the bottom edge.
     */
    val single: Boolean = false,
) {
    /** o₁: the tallest stop's offset. */
    val top: Float get() = offsets[0]

    /** oₙ: the lowest stop's offset. */
    val lowest: Float get() = offsets[offsets.size - 1]

    val count: Int get() = offsets.size

    fun has(detent: SheetDetent): Boolean = detents.contains(detent)

    fun indexOf(detent: SheetDetent): Int = detents.indexOf(detent)

    /** The offset of [detent], or NaN when it is not an active stop. */
    fun offsetOf(detent: SheetDetent): Float {
        val index = detents.indexOf(detent)
        return if (index < 0) Float.NaN else offsets[index]
    }

    /** The stop nearest [offset]. */
    fun nearestIndex(offset: Float): Int {
        var best = 0
        for (index in 1 until offsets.size) {
            if (abs(offsets[index] - offset) < abs(offsets[best] - offset)) best = index
        }
        return best
    }

    /** True when [offset] is on a stop, within half a pixel. */
    fun isAtStop(offset: Float): Boolean {
        for (stop in offsets) if (abs(stop - offset) < RestPx) return true
        return false
    }

    override fun equals(other: Any?): Boolean = other is SheetStops &&
        detents == other.detents && offsets.contentEquals(other.offsets) && hidden == other.hidden &&
        largeTop == other.largeTop && restBottom == other.restBottom && largeIndex == other.largeIndex &&
        roomAbove == other.roomAbove && span == other.span && single == other.single

    override fun hashCode(): Int {
        var result = detents.hashCode()
        result = 31 * result + offsets.contentHashCode()
        result = 31 * result + hidden.hashCode()
        result = 31 * result + largeTop.hashCode()
        result = 31 * result + restBottom.hashCode()
        return 31 * result + largeIndex
    }
}

/**
 * The stops of a sheet at the bottom edge, or of a non-modal side panel.
 * Heights are measured up from [DetentInputs.restBottom] and clamped between the minimum and Large's
 * height; Large and Medium are placed absolutely. Detents within a pixel of each other merge into one
 * stop, Large's identity winning; an inactive detent is left out; a sheet whose detents are all
 * inactive gets Large.
 */
internal fun resolveStops(detents: List<SheetDetent>, inputs: DetentInputs): SheetStops {
    val resolver = DetentResolver(inputs)
    val accepted = ArrayList<SheetDetent>(detents.size)
    val positions = FloatArray(detents.size + 1)
    for (detent in detents) {
        val offset = resolver.offsetOf(detent) ?: continue
        var same = -1
        for (index in accepted.indices) if (abs(positions[index] - offset) < MergePx) same = index
        if (same >= 0) {
            if (detent == SheetDetent.Large) accepted[same] = SheetDetent.Large
            continue
        }
        positions[accepted.size] = offset
        accepted += detent
    }
    if (accepted.isEmpty()) {
        positions[0] = inputs.largeTop
        accepted += SheetDetent.Large
    }
    val order = accepted.indices.sortedBy { positions[it] }
    val offsets = FloatArray(order.size) { positions[order[it]] }
    var largeIndex = -1
    for (index in offsets.indices) if (abs(offsets[index] - inputs.largeTop) < MergePx) largeIndex = index
    val stretchLine = inputs.largeTop - DesignTokens.Sheet.Stretch.aboveLarge * inputs.boundsHeight
    return SheetStops(
        detents = order.map { accepted[it] },
        offsets = offsets,
        hidden = inputs.boundsHeight,
        largeTop = inputs.largeTop,
        restBottom = inputs.restBottom,
        largeIndex = largeIndex,
        roomAbove = offsets[0] - stretchLine,
    )
}

/**
 * The one stop of a form sheet or a modal side panel at [rest], which carries [recorded], the
 * detent the sheet takes back when the window returns it to the bottom edge. [roomAbove] is how far
 * it gives when pulled away from hidden; [span] the bounds' length along its travel.
 */
internal fun singleStop(
    recorded: SheetDetent,
    rest: Float,
    hidden: Float,
    roomAbove: Float,
    span: Float,
    inputs: DetentInputs,
): SheetStops = SheetStops(
    detents = listOf(recorded),
    offsets = floatArrayOf(rest),
    hidden = hidden,
    largeTop = inputs.largeTop,
    restBottom = inputs.restBottom,
    largeIndex = -1,
    roomAbove = roomAbove,
    span = span,
    single = true,
)

/**
 * Where [detent] would put the sheet in this context even when it is inactive: the stop a restored
 * or requested detent that is not active lands nearest. Null for a custom detent that resolves
 * to nothing.
 */
internal fun nominalOffset(detent: SheetDetent, inputs: DetentInputs): Float? =
    DetentResolver(inputs).offsetOf(detent, nominal = true)

/**
 * The stop a non-modal sheet's `upThrough` maps to. An active detent maps to itself; an
 * inactive one to the highest active stop no taller than it, else the lowest stop.
 */
internal fun boundaryIndex(upThrough: SheetDetent, stops: SheetStops, inputs: DetentInputs): Int {
    val own = stops.indexOf(upThrough)
    if (own >= 0) return own
    val nominal = nominalOffset(upThrough, inputs) ?: return stops.count - 1
    for (index in 0 until stops.count) {
        if (stops.offsets[index] >= nominal - MergePx) return index
    }
    return stops.count - 1
}

private class DetentResolver(private val inputs: DetentInputs) : SheetDetentContext {
    override val density: Float get() = inputs.density
    override val fontScale: Float get() = inputs.fontScale

    private val maxPx = (inputs.restBottom - inputs.largeTop).coerceAtLeast(0f)
    private val minPx = (DesignTokens.Sheet.Detent.minHeight.value * inputs.density).coerceAtMost(maxPx)

    override val maxHeight: Dp get() = maxPx.toDp()
    override val containerHeight: Dp get() = inputs.boundsHeight.toDp()
    override val contentHeight: Dp get() = inputs.contentHeight.toDp()
    override val headerHeight: Dp get() = inputs.headerHeight.toDp()
    override val sizeClass: SizeClass get() = inputs.sizeClass
    override val foldBottom: Dp? get() = inputs.foldBottom?.toDp()

    override fun heightOf(detent: SheetDetent): Dp? =
        if (detent is SheetDetent.Custom) null else offsetOf(detent)?.let { (inputs.restBottom - it).toDp() }

    fun offsetOf(detent: SheetDetent, nominal: Boolean = false): Float? {
        if (detent == SheetDetent.Large) return inputs.largeTop
        if (detent == SheetDetent.Medium) return mediumOffset(nominal)
        val height = when (detent) {
            SheetDetent.FitContent -> inputs.contentHeight
            SheetDetent.Peek -> inputs.headerHeight
            is SheetDetent.Fraction -> detent.fraction * maxPx
            is SheetDetent.Height -> detent.height.value * inputs.density
            is SheetDetent.Custom -> detent.resolve(this)?.let { it.value * inputs.density } ?: return null
        }
        return clampToFold(inputs.restBottom - height.coerceIn(minPx, maxPx))
    }

    private fun mediumOffset(nominal: Boolean): Float? {
        if (!nominal && inputs.sizeClass.height == WindowHeight.Compact) return null
        val fold = inputs.foldBottom
        val raw = fold ?: (inputs.boundsHeight * DesignTokens.Sheet.Detent.mediumFraction)
        val offset = raw.coerceIn(inputs.largeTop, inputs.restBottom - minPx)
        val gap = DesignTokens.Sheet.Detent.mediumMinGap.value * inputs.density
        return if (!nominal && offset - inputs.largeTop < gap) null else offset
    }

    /** No partial stop straddles a tabletop crease; a stop as tall as Large may. */
    private fun clampToFold(offset: Float): Float {
        val fold = inputs.foldBottom ?: return offset
        return if (offset - inputs.largeTop < MergePx) offset else maxOf(offset, fold)
    }
}

/** Detents this close are one stop, so `[FitContent, Large]` with a full list is one stop. */
internal const val MergePx = 1f

/** A sheet within this of a stop is resting on it. */
internal const val RestPx = 0.5f
