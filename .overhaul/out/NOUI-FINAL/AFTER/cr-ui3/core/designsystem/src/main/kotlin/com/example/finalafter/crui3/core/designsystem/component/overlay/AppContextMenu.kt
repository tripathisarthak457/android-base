package com.example.finalafter.crui3.core.designsystem.component.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.finalafter.crui3.core.designsystem.R
import com.example.finalafter.crui3.core.designsystem.component.list.AppRow
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.AppHaptic
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.ContainerActions
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.LocalContainerActions
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.containerActionsSemantics
import com.example.finalafter.crui3.core.designsystem.foundation.interaction.rememberAppHaptics
import com.example.finalafter.crui3.core.designsystem.icon.AppIcons
import com.example.finalafter.crui3.core.designsystem.preview.AppComponentPreviews
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * [content] with a menu behind it, as iOS's context menu: a long press lifts the content out of the
 * page, dims the window and opens the menu beside it, below when there is room and above when not.
 * A right click opens the menu at the pointer instead, as a desktop does, without the lift. The
 * menu is [AppMenuItem]s, with [AppMenuDivider] between groups; choosing one closes it. A screen
 * reader opens it as the long-click action of the control inside, such as `AppRow` or a clickable
 * `AppCard`, so make the content one.
 *
 * ```
 * AppContextMenu(menu = { AppMenuItem(share, onClick = onShare, icon = AppIcons.Share) }) {
 *     AppRow(title = item.title, onClick = { onOpen(item) })
 * }
 * ```
 *
 * The lifted copy is the content's own drawing, recorded each time the content draws, so opening
 * the menu recomposes nothing on the page; the lift and the dim move in the layer and in draw.
 */
@Composable
fun AppContextMenu(
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val state = remember { ContextMenuState() }
    val drawing = rememberGraphicsLayer()
    val haptics = rememberAppHaptics()
    val showMenu = stringResource(R.string.designsystem_show_menu)
    // Actions of a container further out, such as a swipe row's, travel with the menu's.
    val outer = LocalContainerActions.current
    val actions = remember(showMenu, enabled, outer) {
        if (enabled) {
            ContainerActions(showMenu, { state.open(ContextMenuOpening.Lift) }, outer?.actions.orEmpty())
        } else {
            outer
        }
    }
    Box(
        modifier = modifier
            .containerActionsSemantics(actions)
            .onPlaced { state.coordinates = it }
            .drawWithContent {
                drawing.record { this@drawWithContent.drawContent() }
                // While lifted the copy in the overlay stands in for the content, as on iOS.
                if (!state.lifted) drawLayer(drawing)
            }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectContextMenu(
                                onLongPress = {
                                    haptics.perform(AppHaptic.LongPress)
                                    state.open(ContextMenuOpening.Lift)
                                },
                                onSecondaryClick = { state.open(ContextMenuOpening.AtPointer, it) },
                            )
                        }
                } else {
                    Modifier
                },
            ),
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalContainerActions provides actions, content = content)
    }
    ContextMenuOverlay(state, drawing, menu)
}

private enum class ContextMenuOpening { Lift, AtPointer }

@Stable
private class ContextMenuState {
    var coordinates: LayoutCoordinates? = null

    /** How the menu is open, or null while it is closed. Read only by the overlay's composition. */
    var opening by mutableStateOf<ContextMenuOpening?>(null)
        private set

    /** Whether the page leaves the content out because the overlay draws it. Read only in draw. */
    var lifted by mutableStateOf(false)
        private set

    /** Where the menu grows from: the side of it nearest the content or the pointer. */
    var menuOrigin by mutableStateOf(TransformOrigin(0f, 0f))

    /** The content's bounds in the window when the menu opened, and where a right click landed in it. */
    var anchor = Rect.Zero
        private set
    var pointer = Offset.Zero
        private set

    fun open(how: ContextMenuOpening, at: Offset = Offset.Zero) {
        val placed = coordinates?.takeIf { it.isAttached } ?: return
        if (opening != null) return
        anchor = placed.boundsInRoot()
        pointer = at
        opening = how
    }

    /**
     * Called once the overlay has drawn its copy: hiding the content any sooner would leave a frame
     * with neither on screen.
     */
    fun copyShown() {
        lifted = opening == ContextMenuOpening.Lift
    }

    fun closed() {
        lifted = false
        opening = null
    }
}

@Composable
private fun ContextMenuOverlay(state: ContextMenuState, drawing: GraphicsLayer, menu: @Composable () -> Unit) {
    val opening = state.opening ?: return
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    val scope = rememberCoroutineScope()
    val lift = remember { Animatable(0f) }
    val shown = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            launch { shown.animateTo(0f, springs.dialog.unit) }
            // The lift runs back from wherever it is, at the speed it had, so a quick dismissal
            // during the opening turns around instead of finishing first.
            lift.animateTo(0f, springs.dialog.unit)
            state.closed()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            withFrameNanos {}
            state.copyShown()
        }
        launch { shown.animateTo(1f, springs.dialog.unit) }
        lift.animateTo(1f, if (reduceMotion) springs.dialog.unit else springs.emphasis.unit)
    }
    OverlayBackHandler(enabled = !closing) { close() }

    HostedOverlay(modal = { true }) {
        val colors = AppTheme.colors
        val lifting = opening == ContextMenuOpening.Lift
        val liftScale = if (reduceMotion) 1f else LiftScale
        val previewShape = AppTheme.shapes.md
        val insets = WindowInsets.safeDrawing
        Layout(
            modifier = Modifier
                .drawBehind { if (lifting) drawRect(colors.scrim, alpha = lift.value * ScrimFraction) }
                .pointerInput(Unit) { detectTapGestures { close() } },
            content = {
                if (lifting) {
                    Box(
                        Modifier
                            .graphicsLayer {
                                val p = lift.value
                                scaleX = lerp(1f, liftScale, p)
                                scaleY = scaleX
                                shadowElevation = AppContextMenuDefaults.PreviewShadow.toPx() * p.coerceIn(0f, 1f)
                                shape = previewShape
                                clip = true
                                ambientShadowColor = colors.shadow
                                spotShadowColor = colors.shadow
                            }
                            .drawBehind {
                                drawRect(colors.surface)
                                drawLayer(drawing)
                            },
                    )
                }
                MenuPanel(
                    onChoose = ::close,
                    modifier = Modifier.graphicsLayer {
                        val p = shown.value
                        alpha = p.coerceIn(0f, 1f)
                        scaleX = MenuStartScale + (1f - MenuStartScale) * p
                        scaleY = scaleX
                        transformOrigin = state.menuOrigin
                    },
                    content = menu,
                )
            },
        ) { measurables, constraints ->
            val anchor = state.anchor
            val previewPlaceable = if (lifting) {
                measurables.first().measure(Constraints.fixed(anchor.width.roundToInt(), anchor.height.roundToInt()))
            } else {
                null
            }
            val menuPlaceable = measurables.last().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val margin = AppContextMenuDefaults.Margin.roundToPx()
            val gap = AppContextMenuDefaults.Gap.roundToPx()
            val top = maxOf(insets.getTop(this), margin)
            val bottom = height - maxOf(insets.getBottom(this), margin)
            val menuWidth = menuPlaceable.width
            val menuHeight = menuPlaceable.height
            val rtl = layoutDirection == LayoutDirection.Rtl

            layout(width, height) {
                val origin = coordinates?.positionInRoot() ?: Offset.Zero
                val left = (anchor.left - origin.x).roundToInt()
                val anchorTop = (anchor.top - origin.y).roundToInt()
                val anchorHeight = anchor.height.roundToInt()
                if (previewPlaceable != null) {
                    // The content stays where it was when the menu fits next to it, and otherwise
                    // rises just enough for the menu to fit below it.
                    val roomBelow = bottom - (anchorTop + anchorHeight + gap)
                    val roomAbove = anchorTop - gap - top
                    val menuBelow = menuHeight <= roomBelow || menuHeight > roomAbove
                    val previewTarget = if (menuHeight <= roomBelow || menuHeight <= roomAbove) {
                        anchorTop
                    } else {
                        (bottom - menuHeight - gap - anchorHeight).coerceAtLeast(top)
                    }
                    val previewTop = lerp(anchorTop.toFloat(), previewTarget.toFloat(), lift.value.coerceIn(0f, 1f)).roundToInt()
                    previewPlaceable.place(left, previewTop)
                    val menuTop = if (menuBelow) {
                        (previewTarget + anchorHeight + gap).coerceAtMost(bottom - menuHeight)
                    } else {
                        anchorTop - gap - menuHeight
                    }
                    val leading = if (rtl) left + previewPlaceable.width - menuWidth else left
                    val menuLeft = leading.coerceIn(margin, (width - margin - menuWidth).coerceAtLeast(margin))
                    state.menuOrigin = TransformOrigin(
                        ((left + previewPlaceable.width / 2f - menuLeft) / menuWidth.coerceAtLeast(1)).coerceIn(0f, 1f),
                        if (menuBelow) 0f else 1f,
                    )
                    menuPlaceable.place(menuLeft, menuTop)
                } else {
                    val x = (left + state.pointer.x).roundToInt()
                    val y = (anchorTop + state.pointer.y).roundToInt()
                    val fitsRight = x + menuWidth <= width - margin
                    val fitsBelow = y + menuHeight <= bottom
                    val menuLeft = (if (fitsRight) x else x - menuWidth).coerceIn(margin, (width - margin - menuWidth).coerceAtLeast(margin))
                    val menuTop = (if (fitsBelow) y else y - menuHeight).coerceIn(top, (bottom - menuHeight).coerceAtLeast(top))
                    state.menuOrigin = TransformOrigin(if (fitsRight) 0f else 1f, if (fitsBelow) 0f else 1f)
                    menuPlaceable.place(menuLeft, menuTop)
                }
            }
        }
    }
}

/**
 * A long press with a finger, or a right click with a mouse. Watching from the outside in, so the
 * content's own press feedback still shows; once either is recognised, the rest of the gesture is
 * taken, so the content's tap never fires and the list beneath does not scroll.
 */
private suspend fun PointerInputScope.detectContextMenu(onLongPress: () -> Unit, onSecondaryClick: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (down.type == PointerType.Mouse) {
            if (!currentEvent.buttons.isSecondaryPressed) return@awaitEachGesture
            takeRestOfGesture()
            onSecondaryClick(down.position)
            return@awaitEachGesture
        }
        val endedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                // The content's own press takes the down, so consumption says nothing here; a scroll
                // or a swipe starts only past the touch slop, which ends the wait just the same.
                val change = awaitPointerEvent(PointerEventPass.Final).changes.firstOrNull { it.id == down.id }
                if (change == null || !change.pressed) break
                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) break
            }
            true
        }
        if (endedEarly == true) return@awaitEachGesture
        onLongPress()
        takeRestOfGesture()
    }
}

private suspend fun AwaitPointerEventScope.takeRestOfGesture() {
    currentEvent.changes.forEach { it.consume() }
    do {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
    } while (event.changes.any { it.pressed })
}

/** The lifted content grows by this much, enough to read as picked up without covering its neighbours. */
private const val LiftScale = 1.03f

/** The window dims to half the scrim an alert uses: the page stays readable around the menu. */
private const val ScrimFraction = 0.5f

/**
 * The lifted preview's shadow, and how far the menu keeps from the window's edges and from the
 * preview.
 */
object AppContextMenuDefaults {
    val PreviewShadow = 16.dp
    val Margin = 16.dp
    val Gap = 8.dp
}

@AppComponentPreviews
@Composable
private fun AppContextMenuPreview() {
    Column(modifier = Modifier.background(AppTheme.colors.background)) {
        AppContextMenu(
            menu = {
                AppMenuItem(text = "Copy", onClick = {}, icon = AppIcons.Copy)
                AppMenuItem(text = "Delete", onClick = {}, icon = AppIcons.Trash, destructive = true)
            },
        ) {
            AppRow(title = "Press and hold for actions", subtitle = "A context menu at rest")
        }
        AppContextMenu(menu = {}, enabled = false) {
            AppRow(title = "No actions here")
        }
    }
}
