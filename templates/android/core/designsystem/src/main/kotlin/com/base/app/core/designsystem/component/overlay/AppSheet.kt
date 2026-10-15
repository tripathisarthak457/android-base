package com.base.app.core.designsystem.component.overlay

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.button.AppButtonDefaults
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.layout.ChromeInset
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.foundation.adaptive.WindowHeight
import com.base.app.core.designsystem.foundation.adaptive.WindowPosture
import com.base.app.core.designsystem.foundation.adaptive.WindowWidth
import com.base.app.core.designsystem.foundation.interaction.AppHaptic
import com.base.app.core.designsystem.foundation.interaction.rememberAppHaptics
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A sheet over everything beneath it, as iOS draws one: it rises to its first detent, can be dragged
 * between detents or down to close, and scrolling inside hands off to the sheet at either end. A tap
 * on the dimmed screen behind, Back, or the close button closes it; [onDismiss] runs once it has
 * gone, for the presenter to remove it.
 *
 * The content is measured once, at the large detent's height, and only moved after that, so dragging
 * the sheet re-places it and never re-measures it. A [footer] (an `AppBottomActionBar`) stays at the
 * bottom of the part that is showing.
 *
 * The content runs edge to edge, as a screen's does: an `AppSection` insets itself, and anything else
 * pads with `AppTheme.spacing.gutter`.
 */
@Composable
fun AppSheet(
    state: AppSheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    showCloseButton: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    SheetBack(state, onDismiss)
    SheetLayout(state, onDismiss, modifier, title, showCloseButton, footer, content)
}

/**
 * Back for a sheet. The system gesture shrinks it toward its bottom edge as the finger moves, as
 * Material's bottom sheets do (iOS has no back gesture to match; dragging the sheet down is still
 * the touch way to close it), and closes it on release. While it is already leaving, Back is taken
 * and ignored, so the screen beneath is not popped as well.
 */
@Composable
private fun SheetBack(state: AppSheetState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val gone by rememberUpdatedState(onDismiss)
    OverlayBackHandler(
        enabled = true,
        onProgress = { if (!state.dismissing) state.backProgress = it },
        onCancel = { scope.launch { state.cancelBack() } },
        onBack = { if (!state.dismissing) scope.launch { state.dismiss(gone) } },
    )
}

@Composable
private fun SheetLayout(
    state: AppSheetState,
    onDismiss: () -> Unit,
    modifier: Modifier,
    title: String?,
    showCloseButton: Boolean,
    footer: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val springs = AppTheme.springs
    val reduceMotion = AppTheme.reduceMotion
    val haptics = rememberAppHaptics()
    val scope = rememberCoroutineScope()
    val gone by rememberUpdatedState(onDismiss)
    val statusBar = WindowInsets.statusBars
    val layout = AppTheme.layout
    val container = AppTheme.containerClass
    // On a window with room on every side the sheet is a card in the middle, as iPad's form sheet
    // is; on one too short for two heights it has only one, the tallest. On a foldable in tabletop
    // it rises from the bottom and rests on the fold, rather than as a card across the crease.
    val posture = AppTheme.posture
    val fold = posture.hinge?.takeIf { posture.kind == WindowPosture.Kind.Tabletop }
    val form = container.width >= WindowWidth.Medium && container.height >= WindowHeight.Medium && fold == null
    val oneHeight = container.isShort
    // The sheet's content pads for its own footer, not for the tab bar it covers.
    val bottomChrome = remember { ChromeInset() }
    state.spring = springs.sheet
    state.rubberBand = springs.rubberBand

    LaunchedEffect(state) { state.show(animate = !reduceMotion) }

    fun close() {
        scope.launch { state.dismiss(gone) }
    }

    val connection = remember(state) {
        object : NestedScrollConnection {
            // Scrolling up grows the sheet to its large detent before the content starts to scroll.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (available.y < 0f && state.canExpand) Offset(0f, state.dragBy(available.y)) else Offset.Zero

            // Content already at its top: pulling further down moves the sheet instead.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset =
                if (available.y > 0f && source == NestedScrollSource.UserInput) Offset(0f, state.dragBy(available.y)) else Offset.Zero

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.resting) return Velocity.Zero
                if (state.settle(available.y) { haptics.perform(AppHaptic.Threshold) }) gone()
                return available
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                // A drawn rect, so the dim never takes the window offscreen while it fades.
                .drawBehind { drawRect(colors.scrim, alpha = state.progress) }
                .pointerInput(state) { detectTapGestures { close() } },
        )
        Layout(
            contents = listOf(
                {
                    Column(
                        modifier = modifier
                            .graphicsLayer {
                                val back = state.backProgress
                                if (back > 0f) {
                                    transformOrigin = BottomCentre
                                    scaleX = 1f - BackShrinkX.toPx() * back / size.width
                                    scaleY = 1f - BackShrinkY.toPx() * back / size.height
                                }
                            }
                            .clip(if (form) AppTheme.shapes.xl else AppTheme.shapes.sheet)
                            .background(colors.surface)
                            .semantics { title?.let { paneTitle = it } }
                            .nestedScroll(connection)
                            .draggable(
                                state = rememberDraggableState { delta -> state.dragBy(delta) },
                                orientation = Orientation.Vertical,
                                enabled = !state.dismissing,
                                onDragStarted = { state.stop() },
                                onDragStopped = { velocity ->
                                    if (state.settle(velocity) { haptics.perform(AppHaptic.Threshold) }) gone()
                                },
                            ),
                    ) {
                        SheetHeader(title = title, showCloseButton = showCloseButton || form, onClose = ::close)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .consumeWindowInsets(WindowInsets.statusBars)
                                .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom)),
                        ) {
                            CompositionLocalProvider(LocalBottomChrome provides bottomChrome, content = content)
                        }
                    }
                },
                { footer?.invoke() },
            ),
        ) { (sheetSlot, footerSlot), constraints ->
            val height = constraints.maxHeight
            val sheetHeight: Int
            val width: Int
            val anchors: SheetAnchors
            if (form) {
                width = minOf(layout.formSheetWidth.roundToPx(), constraints.maxWidth - 2 * layout.gutter.roundToPx())
                sheetHeight = minOf((height * FormHeightFraction).roundToInt(), FormSheetMaxHeight.roundToPx())
                val centred = ((height - sheetHeight) / 2).toFloat()
                anchors = SheetAnchors(large = centred, medium = centred, hidden = height.toFloat(), form = true)
            } else {
                width = minOf(constraints.maxWidth, layout.sheetMaxWidth.roundToPx())
                val large = (statusBar.getTop(this) + LargeGap.roundToPx()).toFloat()
                val medium = when {
                    oneHeight -> large
                    fold != null -> fold.bottom.coerceIn(large, height.toFloat())
                    else -> height * (1f - MediumFraction)
                }
                sheetHeight = (height - large).roundToInt().coerceAtLeast(0)
                anchors = SheetAnchors(large = large, medium = medium, hidden = height.toFloat(), form = false)
            }
            state.updateAnchors(anchors)
            val pinned = footerSlot.firstOrNull()?.measure(Constraints(maxWidth = width))
            bottomChrome.value = (pinned?.height ?: 0).toDp()
            val sheet = sheetSlot.first().measure(Constraints.fixed(width, sheetHeight))
            layout(constraints.maxWidth, height) {
                val x = (constraints.maxWidth - width) / 2
                val top = if (state.offset.isNaN()) height.toFloat() else state.offset
                sheet.place(x, top.roundToInt())
                val bottom = if (form) top.roundToInt() + sheetHeight else height
                pinned?.place(x, maxOf(bottom - pinned.height, top.roundToInt()))
            }
        }
    }
}

/**
 * A sheet shown from inside a screen, for a quick choice that is not a destination of its own. Keep
 * it composed and drive [visible]; it renders in the window's overlay layer, over the tab bar.
 * Sheets that are destinations are registered with the navigation graph instead and presented by the
 * shell.
 */
@Composable
fun AppSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    detents: Set<SheetDetent> = setOf(SheetDetent.Medium, SheetDetent.Large),
    title: String? = null,
    showCloseButton: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    var session by remember { mutableIntStateOf(0) }
    var mounted by remember { mutableStateOf(visible) }
    val state = remember(session) { AppSheetState(detents) }
    LaunchedEffect(visible) {
        if (visible) {
            if (!mounted) {
                session++
                mounted = true
            }
        } else if (mounted) {
            state.dismiss()
            mounted = false
        }
    }
    if (!mounted) return
    SheetBack(state, onDismissRequest)
    HostedOverlay(modal = true) {
        SheetLayout(state, onDismissRequest, modifier, title, showCloseButton, footer, content)
    }
}

@Composable
private fun SheetHeader(title: String?, showCloseButton: Boolean, onClose: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp)
                .size(width = GrabberWidth, height = 5.dp)
                .background(AppTheme.colors.contentTertiary.copy(alpha = 0.45f), CircleShape)
                .testTag("sheet_grabber"),
        )
        if (title != null || showCloseButton) {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp, start = AppTheme.spacing.gutter, end = AppTheme.spacing.xs).height(44.dp)) {
                title?.let {
                    AppText(
                        text = it,
                        modifier = Modifier.align(Alignment.Center),
                        style = AppTheme.typography.headline,
                        color = AppTheme.colors.contentPrimary,
                        maxLines = 1,
                    )
                }
                if (showCloseButton) {
                    AppIconButton(
                        icon = AppIcons.Close,
                        contentDescription = stringResource(R.string.designsystem_close),
                        onClick = onClose,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        variant = ButtonVariant.Tinted,
                        size = ButtonSize.Small,
                        colors = AppButtonDefaults.iconButtonColors(ButtonVariant.Tinted, content = AppTheme.colors.contentSecondary),
                    )
                }
            }
        } else {
            Box(Modifier.height(HandleArea))
        }
    }
}

/** How much of the window a sheet covers at its medium detent. */
private const val MediumFraction = 0.55f
private const val FormHeightFraction = 0.85f
private val FormSheetMaxHeight = 620.dp

/** How far the system back gesture narrows and lowers the sheet at its end: Material's bottom container. */
private val BackShrinkX = 48.dp
private val BackShrinkY = 24.dp
private val BottomCentre = TransformOrigin(0.5f, 1f)
private val LargeGap = 10.dp
private val HandleArea = 20.dp
private val GrabberWidth = 36.dp
