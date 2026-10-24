package com.example.everything.core.designsystem.component.sheet.layout

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.isSpecified
import com.example.everything.core.designsystem.component.sheet.SheetController
import com.example.everything.core.designsystem.component.sheet.SheetModality
import com.example.everything.core.designsystem.component.sheet.SheetPlacement
import com.example.everything.core.designsystem.foundation.adaptive.AppLayout
import com.example.everything.core.designsystem.foundation.adaptive.WindowHeight
import com.example.everything.core.designsystem.foundation.adaptive.WindowWidth
import com.example.everything.core.designsystem.theme.tokens.DesignTokens
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The sheet layer's measure: its frame from the window and the placement, the grabber band and the
 * header, the content at Large's height, the footer, and from those the stops.
 *
 * The content is measured once per settle and never during a drag: nothing here reads a value that
 * changes per frame. The keyboard's target sizes the content box to what is visible above it, but
 * the stops always come from the keyboard-free measure, so a keyboard never moves a detent.
 */
internal class SheetBodyPolicy(private val controller: SheetController) : MultiContentMeasurePolicy {

    override fun MeasureScope.measure(measurables: List<List<Measurable>>, constraints: Constraints): MeasureResult {
        val (grabberSlot, headerSlot, contentSlot, footerSlot) = measurables
        val state = controller.sheetState
        val environment = controller.measureEnvironment
        val style = controller.style
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val bounds = controller.boundsInWindow
        val window = controller.windowSize
        val ltr = layoutDirection == LayoutDirection.Ltr

        val topInset = ((environment?.safeTop(this) ?: 0f) - bounds.top).coerceAtLeast(0f)
        val leftInset = ((environment?.safeLeft(this, layoutDirection) ?: 0f) - bounds.left).coerceAtLeast(0f)
        val rightInset = ((environment?.safeRight(this, layoutDirection) ?: 0f) - (window.width - bounds.right)).coerceAtLeast(0f)
        val navigation = ((environment?.navigationBottom(this) ?: 0f) - (window.height - bounds.bottom)).coerceAtLeast(0f)
        val buttons = environment?.buttonNavigation(this) == true

        val placement = controller.placement
        val sizeClass = controller.placementClass
        val tokens = AppLayout.forClass(sizeClass)
        val gutter = tokens.gutter.toPx()
        val modal = controller.modality is SheetModality.Modal
        val aboveBar = (controller.modality as? SheetModality.NonModal)?.aboveBottomChrome == true
        val bottom = placement == SheetPlacement.Bottom
        val panel = placement == SheetPlacement.Start || placement == SheetPlacement.End
        val sidePanel = panel && modal
        val fromLeft = (placement == SheetPlacement.Start) == ltr
        val radius = style?.cornerRadius?.toPx() ?: 0f
        val styleInset = style?.floatingInset?.toPx() ?: 0f
        val maxWidth = style?.maxWidth?.takeIf { it.isSpecified }?.toPx() ?: Float.POSITIVE_INFINITY
        val sideInset = DesignTokens.Sheet.Side.inset.toPx()
        val compactHeight = sizeClass.height == WindowHeight.Compact

        val sheetWidth = when {
            bottom -> {
                var room = minOf(width - leftInset - rightInset, tokens.sheetMaxWidth.toPx())
                if (sizeClass.width >= WindowWidth.Medium) room = minOf(room, width - 2 * gutter)
                minOf(room, maxWidth)
            }
            placement == SheetPlacement.Center -> minOf(tokens.formSheetWidth.toPx(), width - 2 * gutter, maxWidth)
            else -> minOf(tokens.supportingPaneWidth.toPx(), width - leftInset - rightInset - 2 * sideInset, maxWidth)
        }.coerceAtLeast(0f)
        val sheetLeft = when {
            !panel -> leftInset + (width - leftInset - rightInset - sheetWidth) / 2f
            fromLeft -> leftInset + sideInset
            else -> width - rightInset - sideInset - sheetWidth
        }

        val bar = environment?.barFrame?.takeIf { bottom && aboveBar }
        val restBottom = when {
            bar != null -> (bar.settledTop - bounds.top).coerceIn(0f, height)
            bottom || placement == SheetPlacement.Center -> height - navigation
            else -> height - navigation - sideInset
        }
        val largeTop = if (bottom) {
            maxOf(topInset + DesignTokens.Sheet.Detent.largeTopGap.toPx(), controller.stackedLargeTop() - bounds.top)
        } else {
            topInset + sideInset
        }

        val canFloat = bottom && styleInset > 0f && !buttons && !compactHeight
        val headerInset = if (canFloat) styleInset else 0f
        val headerWidth = (sheetWidth - 2 * headerInset).roundToInt().coerceAtLeast(0)
        val header = headerSlot.firstOrNull()?.measure(Constraints(maxWidth = headerWidth))?.takeIf { it.height > 0 }
        val headerTop = DesignTokens.Sheet.Header.top.toPx()
        val grabberShown = controller.grabberShown
        val band = when {
            header != null -> headerTop + header.height
            controller.properties.contentUnderGrabber -> 0f
            grabberShown -> DesignTokens.Sheet.Grabber.contentTop.toPx()
            else -> 0f
        }

        val footer = footerSlot.firstOrNull()?.measure(Constraints(maxWidth = sheetWidth.roundToInt()))?.takeIf { it.height > 0 }
        val footerHeight = footer?.height?.toFloat() ?: 0f
        val resting = state.stops?.let { stops ->
            state.currentDetent?.let { stops.offsetOf(it) }?.takeIf { !it.isNaN() && !stops.single } ?: stops.lowest
        } ?: largeTop
        val visibleAtRest = if (panel || bottom) (restBottom - resting).coerceAtLeast(0f) else Float.POSITIVE_INFINITY
        // Large text in a short window: the content keeps at least half of what shows, and the footer
        // moves to the end of the scroll instead.
        val footerPinned = footer != null && (header?.height ?: 0) + footerHeight <= visibleAtRest / 2f
        controller.footerPinned = footerPinned

        val keyboard = controller.imeTarget
        val keyboardUp = keyboard > 0f && controller.focusInside
        val bottomInset = height - restBottom
        // How far a floating surface's bottom edge sits above restBottom: the inset over the tab bar,
        // and over the window's edge whatever of the inset the navigation bar does not already cover.
        val floatGap = when {
            !canFloat -> 0f
            bar != null -> styleInset
            else -> (styleInset - bottomInset).coerceAtLeast(0f)
        }
        val pinnedFooter = if (footerPinned) footerHeight else 0f
        // Content that scrolls at rest gets room past its end to bring its last row up to the window's
        // bottom, but only content taller than what shows at rest: a fitted detent shows all of a
        // shorter one, and the room would hide how tall it is.
        val known = controller.contentNatural
        val overflows = !known.isNaN() && known > height - resting - band + 1f
        val reach = if (bottom && controller.scrollsAtRest() && overflows) (resting - largeTop).coerceAtLeast(0f) else 0f
        val bottomPadding = when {
            keyboardUp || !bottom -> pinnedFooter
            else -> pinnedFooter + bottomInset + floatGap + reach
        }
        controller.padding.gutter = gutter.toDp()
        controller.padding.bottom = bottomPadding.toDp()

        val formCap = minOf(height * DesignTokens.Sheet.Form.heightFraction, DesignTokens.Sheet.Form.maxHeight.toPx())
        val panelHeight = (height - topInset - navigation - 2 * sideInset).coerceAtLeast(0f)
        val contentRoom = when {
            keyboardUp && bottom -> {
                val lifted = maxOf(largeTop, resting - (keyboard - bottomInset).coerceAtLeast(0f))
                height - keyboard - lifted - band
            }
            bottom -> height - largeTop - band
            placement == SheetPlacement.Center -> formCap - band
            sidePanel -> panelHeight - band
            else -> restBottom - largeTop - band
        }.coerceAtLeast(0f)
        // While a press prepares the hidden sheet, the content is measured in a room that grows a step per
        // frame, so a lazy list composes its rows over the press instead of in the frame after the tap.
        val prepare = controller.prepare
        val ramping = state.preparing && !state.isVisible
        val room = when {
            !ramping -> contentRoom
            else -> minOf(contentRoom, prepare.room.takeUnless { it.isNaN() } ?: PrepareRamp.FirstStep.toPx())
        }
        val started = if (ramping) System.nanoTime() else 0L
        val content = contentSlot.first().measure(Constraints(maxWidth = sheetWidth.roundToInt(), maxHeight = room.roundToInt()))
        val partial = room.roundToInt() < contentRoom.roundToInt()
        if (ramping) {
            prepare.record(room, contentRoom, nanos = System.nanoTime() - started, ended = content.height < room.roundToInt())
        }
        if (prepare.partial != partial) prepare.partial = partial
        // What the reach added to the content's height, in the content's own whole pixels; nothing
        // when the content does not apply the padding.
        val reachMeasured = if (!keyboardUp && controller.padding.bottomRead) {
            (bottomPadding.toDp().roundToPx() - (bottomPadding - reach).toDp().roundToPx()).toFloat()
        } else {
            0f
        }
        val natural = when {
            content.height < contentRoom.roundToInt() -> content.height - reachMeasured
            reach == 0f -> Float.POSITIVE_INFINITY
            else -> known
        }
        if (!keyboardUp && !partial && (known.isNaN() || (natural != known && abs(natural - known) >= 0.5f))) {
            controller.contentNatural = natural
        }

        val cardHeight = when {
            placement == SheetPlacement.Center -> (band + content.height).coerceAtMost(formCap)
            sidePanel -> panelHeight
            else -> Float.NaN
        }
        if (!keyboardUp) {
            val inputs = DetentInputs(
                boundsHeight = height,
                largeTop = largeTop,
                restBottom = restBottom,
                contentHeight = band + (if (natural.isFinite()) natural else content.height.toFloat()) - if (bottom) bottomInset else 0f,
                headerHeight = if (header != null) band + floatGap else 0f,
                sizeClass = sizeClass,
                foldBottom = if (bottom) foldBottom(controller.fold, bounds.top, height) else null,
                density = density,
                fontScale = fontScale,
            )
            val stops = when {
                placement == SheetPlacement.Center -> {
                    val rest = topInset + ((height - topInset - navigation - cardHeight) / 2f).coerceAtLeast(0f)
                    singleStop(controller.recordedDetent(), rest, hidden = height, roomAbove = rest - topInset, span = height, inputs = inputs)
                }
                sidePanel -> {
                    val edge = if (fromLeft) leftInset else rightInset
                    singleStop(controller.recordedDetent(), 0f, sheetWidth + sideInset + edge, roomAbove = sideInset, span = width, inputs = inputs)
                }
                else -> resolveStops(state.resolved, inputs)
            }
            state.bottomPlaced = bottom
            controller.applyStops(stops, inputs)
        }
        val stops = state.stops

        val layerHeight = if (cardHeight.isNaN()) (if (bottom) height - largeTop else restBottom - largeTop) else cardHeight
        val floats = canFloat && stops != null && stops.count > (if (stops.largeIndex >= 0) 1 else 0)
        controller.updateFrame(
            SheetFrameInfo(
                placement = placement,
                boundsLeft = bounds.left,
                boundsTop = bounds.top,
                boundsWidth = width,
                boundsHeight = height,
                sheetLeft = sheetLeft,
                sheetWidth = sheetWidth,
                layerHeight = layerHeight.coerceAtLeast(0f),
                largeTop = largeTop,
                restBottom = restBottom,
                bottomInset = bottomInset,
                onBar = bar != null,
                floats = floats,
                inset = styleInset,
                radius = radius,
                bandRoom = DesignTokens.Sheet.Stretch.aboveLarge * height,
                bandHeight = band,
                headerTop = headerTop,
                headerHeight = header?.height?.toFloat() ?: 0f,
                footerHeight = footerHeight,
                footerPinned = footerPinned,
                cardHeight = cardHeight,
                horizontal = sidePanel,
                fromLeft = fromLeft,
                recedes = modal && bottom && sizeClass.width == WindowWidth.Compact && !compactHeight && controller.fold == null,
                spansWidth = sheetWidth >= width - 1f,
                scrollEdgeRamp = DesignTokens.Sheet.Header.scrollEdgeRamp.toPx(),
            ),
        )

        val grabber = if (grabberShown) grabberSlot.firstOrNull()?.measure(Constraints()) else null
        val pillCentre = (DesignTokens.Sheet.Grabber.top + DesignTokens.Sheet.Grabber.height / 2).toPx()
        return layout(sheetWidth.roundToInt(), layerHeight.roundToInt().coerceAtLeast(0)) {
            content.placeWithLayer(0, band.roundToInt(), layerBlock = controller.layers.content)
            grabber?.placeWithLayer(((sheetWidth - grabber.width) / 2f).roundToInt(), (pillCentre - grabber.height / 2f).roundToInt())
            header?.placeWithLayer(headerInset.roundToInt(), headerTop.roundToInt())
            if (footerPinned) footer.placeWithLayer(0, 0, layerBlock = controller.layers.footer)
        }
    }
}
