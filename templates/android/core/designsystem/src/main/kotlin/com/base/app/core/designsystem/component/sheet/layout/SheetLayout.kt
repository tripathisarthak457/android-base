package com.base.app.core.designsystem.component.sheet.layout

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.sheet.AppSheetScope
import com.base.app.core.designsystem.component.sheet.SheetController
import com.base.app.core.designsystem.component.sheet.SheetModality
import com.base.app.core.designsystem.component.sheet.back.sheetFocus
import com.base.app.core.designsystem.component.sheet.chrome.SheetGrabberHost
import com.base.app.core.designsystem.component.sheet.chrome.rememberSheetLabels
import com.base.app.core.designsystem.component.sheet.content.SheetContentHost
import com.base.app.core.designsystem.component.sheet.content.SheetFooterHost
import com.base.app.core.designsystem.component.sheet.content.SheetHeaderHost
import com.base.app.core.designsystem.component.sheet.gesture.SheetStretch
import com.base.app.core.designsystem.component.sheet.gesture.sheetCatch
import com.base.app.core.designsystem.component.sheet.surface.SheetScrim
import com.base.app.core.designsystem.component.sheet.surface.sheetSemantics
import com.base.app.core.designsystem.component.sheet.surface.sheetSurface
import kotlin.math.roundToInt

/**
 * One sheet, filling its bounds: the scrim, placed only while it blocks, and the sheet layer, placed
 * at the sheet's offset. Every per-frame value is read in this placement or in layer and draw blocks
 * below it; a drag recomposes nothing and re-measures nothing.
 */
@Composable
internal fun SheetLayout(
    controller: SheetController,
    modifier: Modifier,
    grabber: (@Composable AppSheetScope.() -> Unit)?,
    header: (@Composable AppSheetScope.() -> Unit)?,
    footer: (@Composable AppSheetScope.() -> Unit)?,
    content: @Composable AppSheetScope.() -> Unit,
) {
    val state = controller.sheetState
    val horizontal = controller.frame?.horizontal == true
    val stretch = remember(controller, horizontal) {
        SheetStretch(state, horizontal = horizontal, onDragDelta = controller::onSheetDragDelta)
    }
    val labels = rememberSheetLabels()
    val defaultTitle = stringResource(R.string.designsystem_sheet_pane)
    val slots = controller.slots
    val paneTitle = controller.paneTitle ?: slots.header?.title ?: slots.headerTitle ?: defaultTitle
    Layout(
        contents = listOf(
            { SheetScrim(controller) },
            {
                Layout(
                    contents = listOf(
                        { SheetGrabberHost(controller, grabber) },
                        { SheetHeaderHost(controller, header) },
                        { SheetContentHost(controller, content) },
                        { SheetFooterHost(controller, footer) },
                    ),
                    // Of two semantics on one node the outer wins: the sheet's pane title over a
                    // caller's, a caller's test tag over the default one.
                    modifier = Modifier
                        .sheetSemantics(
                            controller = controller,
                            paneTitle = paneTitle,
                            labels = labels,
                            grabberShown = controller.grabberShown,
                            modal = controller.modality is SheetModality.Modal,
                        )
                        .then(modifier)
                        .testTag(SurfaceTag)
                        .sheetFocus(controller)
                        .sheetCatch(controller)
                        .anchoredDraggable(
                            state = state.anchored,
                            orientation = if (horizontal) Orientation.Horizontal else Orientation.Vertical,
                            reverseDirection = horizontal && controller.frame?.fromLeft == true,
                            enabled = controller.dragEnabled,
                            overscrollEffect = stretch,
                        )
                        .sheetSurface(controller),
                    measurePolicy = remember(controller) { SheetBodyPolicy(controller) },
                )
            },
        ),
        modifier = Modifier.semantics { isTraversalGroup = true },
        measurePolicy = remember(controller) { SheetContainerPolicy(controller) },
    )
}

/** Fills the container, sizes the bounds, and places the scrim and the sheet layer. */
private class SheetContainerPolicy(private val controller: SheetController) : MultiContentMeasurePolicy {
    override fun MeasureScope.measure(measurables: List<List<Measurable>>, constraints: Constraints): MeasureResult {
        val (scrimSlot, bodySlot) = measurables
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        controller.windowSize = Size(width.toFloat(), height.toFloat())
        val bounds = controller.boundsWithin(width.toFloat(), height.toFloat())
        controller.boundsInWindow = bounds
        val boundsWidth = bounds.width.roundToInt()
        val boundsHeight = bounds.height.roundToInt()
        val scrim = scrimSlot.first().measure(Constraints.fixed(boundsWidth, boundsHeight))
        val body = bodySlot.first().measure(Constraints(maxWidth = boundsWidth, maxHeight = boundsHeight))
        return layout(width, height) { place(scrim, body, bounds) }
    }

    private fun Placeable.PlacementScope.place(scrim: Placeable, body: Placeable, bounds: Rect) {
        val state = controller.sheetState
        val frame = controller.frame ?: return
        val origin = coordinates?.positionInWindow()?.y ?: 0f
        if (state.boundsTop != origin + bounds.top) state.boundsTop = origin + bounds.top
        controller.followKeyboard(this, frame)
        controller.followBar(frame)
        if (controller.blocksBeneath()) scrim.place(bounds.left.roundToInt(), bounds.top.roundToInt())
        val offset = state.anchored.offset
        val x: Float
        val y: Float
        if (frame.horizontal) {
            val travel = if (offset.isNaN()) frame.sheetWidth else offset + state.stretch
            x = bounds.left + frame.sheetLeft + if (frame.fromLeft) -travel else travel
            y = bounds.top + frame.largeTop - state.lift
        } else {
            x = bounds.left + frame.sheetLeft
            y = bounds.top + state.placedTop()
        }
        // A parked sheet stays placed, so its layers and display lists survive the tab switch; it is
        // moved off the window and drawn at alpha 0, which the renderer skips.
        val parkedY = if (state.parkedInstantly) controller.windowSize.height + frame.layerHeight else y
        body.placeWithLayer(x.roundToInt(), parkedY.roundToInt(), layerBlock = controller.layers.sheet)
    }
}

/** The sheet's root test tag: the overlay host exposes it as a resource id for the perf scripts. */
internal const val SurfaceTag = "sheet_surface"
