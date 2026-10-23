package io.example.fitclocal.core.designsystem.component.sheet.chrome

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import io.example.fitclocal.core.designsystem.R
import io.example.fitclocal.core.designsystem.component.sheet.AppSheetScope
import io.example.fitclocal.core.designsystem.component.sheet.SheetController
import io.example.fitclocal.core.designsystem.component.sheet.SheetDismissSource
import io.example.fitclocal.core.designsystem.component.sheet.gesture.settleTo
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.tokens.DesignTokens
import kotlinx.coroutines.launch

/**
 * The sheet's grabber: a 48 dp target centred on the pill that owns the tap, Enter and Space,
 * hover, focus and every accessibility action, whatever the grabber slot draws. A tap counts only in
 * the band above the header's top, so it never takes a touch meant for a search field or a segmented
 * control in the header; a drag starting there drags the sheet, as anywhere on it.
 */
@Composable
internal fun SheetGrabberHost(controller: SheetController, grabber: (@Composable AppSheetScope.() -> Unit)?) {
    val slot = controller.slots.style?.grabber ?: grabber ?: return
    // Not composed while hidden, so neither touch nor a screen reader finds a grabber that is not drawn.
    if (!controller.grabberShown) return
    val labels = rememberSheetLabels()
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val ring = AppTheme.colors.focusRing
    val fallback = AppTheme.colors.contentTertiary
    val look = remember(controller) { SheetGrabberLook(controller.style?.colors?.grabber ?: fallback, false) }
    SideEffect {
        controller.style?.colors?.grabber?.let { look.color = it }
        look.contrast = controller.properties.contentUnderGrabber
        look.hovered = hovered
    }
    val state by remember(controller) { derivedStateOf { controller.grabberState() } }
    Box(
        Modifier
            .size(Target)
            .testTag(GrabberTag)
            .semantics {
                role = Role.Button
                contentDescription = labels.grabber
                stateDescription = labels.state(state)
                onClick(label = if (state == GrabberState.Expanded) labels.collapse else labels.expand) {
                    controller.cycleDetent()
                    true
                }
                sheetActions(controller, labels)
            }
            .hoverable(interactions)
            .pointerHoverIcon(PointerIcon.Hand)
            .focusable(interactionSource = interactions)
            .onKeyEvent { event ->
                val activates = event.key == Key.Enter || event.key == Key.Spacebar || event.key == Key.NumPadEnter
                if (activates && event.type == KeyEventType.KeyUp) controller.cycleDetent()
                activates
            }
            .pointerInput(controller) {
                val bandTop = (size.height / 2f) - (DesignTokens.Sheet.Grabber.top + DesignTokens.Sheet.Grabber.height / 2).toPx()
                val bandBottom = bandTop + DesignTokens.Sheet.Grabber.touchBand.toPx()
                detectTapGestures { position -> if (position.y in bandTop..bandBottom) controller.cycleDetent() }
            }
            .drawBehind {
                if (focused) {
                    val ringWidth = RingWidth.toPx()
                    val ringSize = Size(size.width, RingHeight.toPx())
                    drawRoundRect(
                        color = ring,
                        topLeft = Offset(0f, (size.height - ringSize.height) / 2f),
                        size = ringSize,
                        cornerRadius = CornerRadius(ringSize.height / 2f),
                        style = Stroke(ringWidth),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalSheetGrabber provides look) { slot(controller) }
    }
}

/** The state screen readers hear: tallest stop, lowest of three or more, or between. */
internal enum class GrabberState { Expanded, Partial, Collapsed }

internal fun SheetController.grabberState(): GrabberState {
    val stops = sheetState.stops ?: return GrabberState.Partial
    val index = stops.indexOf(sheetState.currentDetent ?: return GrabberState.Partial)
    return when {
        index == 0 -> GrabberState.Expanded
        index == stops.count - 1 && stops.count >= 3 -> GrabberState.Collapsed
        else -> GrabberState.Partial
    }
}

/**
 * The next taller active detent, and from the tallest the lowest. It never dismisses. The
 * arrival haptic plays and the change is reported, as for a drag.
 */
internal fun SheetController.cycleDetent() {
    val state = sheetState
    val stops = state.stops ?: return
    if (stops.single || stops.count < 2 || !state.canMoveProgrammatically()) return
    val index = stops.indexOf(state.currentDetent ?: return)
    val next = if (index <= 0) stops.detents[stops.count - 1] else stops.detents[index - 1]
    scope.launch { state.settleTo(next, velocity = 0f, byPerson = true) }
}

/**
 * The built-in expand, collapse and dismiss actions, each only where it applies, which TalkBack,
 * Switch Access and Voice Access all know. On the grabber node, or on the surface when there is no
 * grabber.
 */
internal fun SemanticsPropertyReceiver.sheetActions(controller: SheetController, labels: SheetLabels) {
    val state = controller.sheetState
    val stops = state.stops
    val index = stops?.indexOf(state.currentDetent ?: return) ?: return
    if (!stops.single && index > 0) {
        expand(labels.expand) {
            controller.scope.launch { state.expand() }
            true
        }
    }
    if (!stops.single && index in 0 until stops.count - 1) {
        collapse(labels.collapse) {
            controller.scope.launch { state.collapse() }
            true
        }
    }
    if (controller.closable) {
        dismiss(labels.dismiss) {
            controller.attemptOrClose(SheetDismissSource.Request)
            true
        }
    }
}

/** The sheet's accessibility strings, read once per composition. */
internal class SheetLabels(
    val grabber: String,
    val expand: String,
    val collapse: String,
    val dismiss: String,
    private val expanded: String,
    private val partial: String,
    private val collapsed: String,
) {
    fun state(state: GrabberState): String = when (state) {
        GrabberState.Expanded -> expanded
        GrabberState.Partial -> partial
        GrabberState.Collapsed -> collapsed
    }
}

@Composable
internal fun rememberSheetLabels(): SheetLabels {
    val grabber = stringResource(R.string.designsystem_sheet_grabber)
    val expand = stringResource(R.string.designsystem_sheet_expand)
    val collapse = stringResource(R.string.designsystem_sheet_collapse)
    val dismiss = stringResource(R.string.designsystem_sheet_dismiss)
    val expanded = stringResource(R.string.designsystem_sheet_state_expanded)
    val partial = stringResource(R.string.designsystem_sheet_state_partial)
    val collapsed = stringResource(R.string.designsystem_sheet_state_collapsed)
    return remember(grabber, expand, collapse, dismiss, expanded, partial, collapsed) {
        SheetLabels(grabber, expand, collapse, dismiss, expanded, partial, collapsed)
    }
}

internal const val GrabberTag = "sheet_grabber"
private val Target = DesignTokens.Sheet.Grabber.touchTarget
private val RingWidth = 2.dp
private val RingHeight = 24.dp
