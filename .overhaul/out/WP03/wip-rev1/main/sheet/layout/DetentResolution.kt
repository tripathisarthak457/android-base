package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.component.sheet.SheetDetent
import com.base.app.core.designsystem.component.sheet.SheetDetentContext
import com.base.app.core.designsystem.foundation.adaptive.SizeClass
import com.base.app.core.designsystem.foundation.adaptive.WindowHeight
import com.base.app.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs

/**
 * Everything settled that a sheet's stops depend on, in pixels from the top of its bounds. None of
 * it changes during a drag, so stops resolve in measure and only when one of these changes.
 */
@Immutable
internal data class DetentInputs(
    /** H: the height of the sheet's bounds. */
    val boundsHeight: Float,
    /** The top of the safe area: the status bar, a caption bar or a cutout. */
    val safeTop: Float,
    /** Large's top: the safe top plus the gap, or the covered sheet's lifted top plus the peek. */
    val largeTop: Float,
    /** The line a resting sheet's content ends at: above the navigation bar or the bottom chrome. */
    val restBottom: Float,
    /** Grabber band, header, content and footer as measured, whatever the detents. */
    val contentHeight: Float,
    val sizeClass: SizeClass,
    /** The bottom of a tabletop fold, or null. */
    val foldBottom: Float?,
    val density: Float,
    val fontScale: Float,
)

/**
 * Where each active detent puts the sheet's top, tallest first ([offsets] ascending), and where the
 * sheet goes when it hides. Two equal sets are the same stops, so anchors are written only when one
 * changed.
 */
@Immutable
internal class SheetStops(
    val detents: List<SheetDetent>,
    val offsets: FloatArray,
    val hidden: Float,
    val largeTop: Float,
    val restBottom: Float,
    /** The visible height of a form sheet, which has one stop; NaN for every other placement. */
    val formHeight: Float = Float.NaN,
) {
    /** o₁: the tallest stop's offset. */
    val top: Float get() = offsets.first()

    /** oₙ: the lowest stop's offset. */
    val lowest: Float get() = offsets.last()

    val isForm: Boolean get() = !formHeight.isNaN()

    fun has(detent: SheetDetent): Boolean = detents.contains(detent)

    /** The offset of [detent], or NaN when it is not an active stop. */
    fun offsetOf(detent: SheetDetent): Float {
        val index = detents.indexOf(detent)
        return if (index < 0) Float.NaN else offsets[index]
    }

    /** The stop nearest [offset]. */
    fun nearest(offset: Float): SheetDetent {
        var best = 0
        for (index in 1 until offsets.size) {
            if (abs(offsets[index] - offset) < abs(offsets[best] - offset)) best = index
        }
        return detents[best]
    }

    override fun equals(other: Any?): Boolean = other is SheetStops &&
        detents == other.detents && offsets.contentEquals(other.offsets) && hidden == other.hidden &&
        largeTop == other.largeTop && restBottom == other.restBottom &&
        (formHeight == other.formHeight || (formHeight.isNaN() && other.formHeight.isNaN()))

    override fun hashCode(): Int {
        var result = detents.hashCode()
        result = 31 * result + offsets.contentHashCode()
        result = 31 * result + hidden.hashCode()
        result = 31 * result + largeTop.hashCode()
        return 31 * result + restBottom.hashCode()
    }
}

/**
 * The stops of a sheet at the bottom or the side: rules R-1 to R-9 of the sheet specification.
 * Heights are measured up from [DetentInputs.restBottom] and clamped to between the minimum and
 * Large's height; Large and Medium are placed absolutely. Detents within a pixel of an earlier one
 * merge into it, an inactive detent is left out, and a sheet whose detents are all inactive gets
 * Large.
 */
internal fun resolveStops(detents: List<SheetDetent>, inputs: DetentInputs): SheetStops {
    val resolver = Resolver(inputs)
    val accepted = ArrayList<SheetDetent>(detents.size)
    val positions = ArrayList<Float>(detents.size)
    for (detent in detents) {
        val offset = resolver.offsetOf(detent) ?: continue
        if (positions.any { abs(it - offset) < MergeDistancePx }) continue
        accepted += detent
        positions += offset
    }
    if (accepted.isEmpty()) {
        accepted += SheetDetent.Large
        positions += inputs.largeTop
    }
    val order = positions.indices.sortedBy { positions[it] }
    return SheetStops(
        detents = order.map { accepted[it] },
        offsets = FloatArray(order.size) { positions[order[it]] },
        hidden = inputs.boundsHeight,
        largeTop = inputs.largeTop,
        restBottom = inputs.restBottom,
    )
}

/**
 * The one stop of a form sheet, centred in the safe area at `min(0.85 H, 620 dp, content)` (R-10).
 * It carries [keep], the detent the sheet rested on before, so a window that grows back to a phone
 * returns it there.
 */
internal fun resolveFormStop(keep: SheetDetent, inputs: DetentInputs): SheetStops {
    val dp = inputs.density
    val height = minOf(
        inputs.boundsHeight * DesignTokens.Sheet.Form.heightFraction,
        DesignTokens.Sheet.Form.maxHeight.value * dp,
        inputs.contentHeight,
    ).coerceAtLeast(DesignTokens.Sheet.Detent.minHeight.value * dp)
    val room = inputs.restBottom - inputs.safeTop
    val top = inputs.safeTop + ((room - height) / 2f).coerceAtLeast(0f)
    return SheetStops(
        detents = listOf(keep),
        offsets = floatArrayOf(top),
        hidden = inputs.boundsHeight,
        largeTop = inputs.largeTop,
        restBottom = inputs.restBottom,
        formHeight = height,
    )
}

/**
 * Where [detent] would put the sheet's top in this context, or null when it is inactive. Used for a
 * restored detent that is no longer listed: the sheet goes to the active stop nearest it.
 */
internal fun offsetOf(detent: SheetDetent, inputs: DetentInputs): Float? = Resolver(inputs).offsetOf(detent)

private class Resolver(private val inputs: DetentInputs) : SheetDetentContext {
    override val density: Float get() = inputs.density
    override val fontScale: Float get() = inputs.fontScale

    private val maxPx = (inputs.restBottom - inputs.largeTop).coerceAtLeast(0f)
    private val minPx = (DesignTokens.Sheet.Detent.minHeight.value * inputs.density).coerceAtMost(maxPx)

    override val maxHeight: Dp get() = maxPx.toDp()
    override val containerHeight: Dp get() = inputs.boundsHeight.toDp()
    override val contentHeight: Dp get() = inputs.contentHeight.toDp()
    override val sizeClass: SizeClass get() = inputs.sizeClass
    override val foldBottom: Dp? get() = inputs.foldBottom?.toDp()

    override fun heightOf(detent: SheetDetent): Dp? =
        if (detent is SheetDetent.Custom) null else heightPx(detent)?.toDp()

    fun offsetOf(detent: SheetDetent): Float? = when (detent) {
        SheetDetent.Large -> inputs.largeTop
        SheetDetent.Medium -> mediumOffset()
        else -> heightPx(detent)?.let { inputs.restBottom - it }
    }

    private fun heightPx(detent: SheetDetent): Float? {
        val raw = when (detent) {
            SheetDetent.Large -> return maxPx
            SheetDetent.Medium -> return mediumOffset()?.let { inputs.restBottom - it }
            SheetDetent.FitContent -> inputs.contentHeight
            is SheetDetent.Fraction -> detent.fraction * maxPx
            is SheetDetent.Height -> detent.height.value * inputs.density
            is SheetDetent.Custom -> detent.resolve(this)?.let { it.value * inputs.density } ?: return null
        }
        return raw.coerceIn(minPx, maxPx)
    }

    private fun mediumOffset(): Float? {
        if (inputs.sizeClass.height == WindowHeight.Compact) return null
        val offset = inputs.foldBottom?.coerceIn(inputs.largeTop, inputs.boundsHeight)
            ?: (inputs.boundsHeight * DesignTokens.Sheet.Detent.mediumFraction)
        val gap = DesignTokens.Sheet.Detent.mediumMinGap.value * inputs.density
        return if (offset - inputs.largeTop < gap) null else offset
    }
}

/** Detents that resolve this close are one stop, so `[FitContent, Large]` with a full list is one stop. */
private const val MergeDistancePx = 1f
