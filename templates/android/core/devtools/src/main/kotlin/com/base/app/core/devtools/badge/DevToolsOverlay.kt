package com.base.app.core.devtools.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.foundation.layout.LocalBottomChrome
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.devtools.DevTools
import com.base.app.core.devtools.page.DevToolsPage
import com.base.app.core.devtools.panel.DevToolsPanel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** The environment badge, and the inspector it opens. Placed last in the root, over every screen. */
@Composable
internal fun DevToolsOverlay(devTools: DevTools) {
    var panelOpen by rememberSaveable { mutableStateOf(false) }
    // The tab a badge line asked for; the panel opens on its own first tab otherwise.
    var requestedTab by rememberSaveable { mutableStateOf<String?>(null) }

    EdgeBadge(
        label = devTools.label,
        pages = devTools.pages,
        onClick = { panelOpen = true },
        onOpenPage = { key ->
            requestedTab = key
            panelOpen = true
        },
    )

    if (panelOpen) {
        DevToolsPanel(
            devTools = devTools,
            initialTab = requestedTab,
            onClose = {
                panelOpen = false
                requestedTab = null
            },
        )
    }
}

/**
 * A tab docked to the side of the window a little below half-way, away from titles, back buttons
 * and the tab bar. It can be dragged anywhere, by finger or mouse, and settles on the side it was
 * thrown towards. The side and the height are kept as a fraction of the free space, so a rotation
 * or a resize keeps it on screen.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EdgeBadge(
    label: String,
    pages: List<DevToolsPage>,
    onClick: () -> Unit,
    onOpenPage: (key: String) -> Unit,
) {
    val position = rememberSaveable(saver = BadgePosition.Saver) { BadgePosition() }
    var window by remember { mutableStateOf(IntSize.Zero) }
    var badge by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val rtl = direction == LayoutDirection.Rtl
    val safe = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val bottomChrome = LocalBottomChrome.current
    // Each screen measures its own top bar, out of reach from the root, so the badge keeps clear
    // of a standard one: dragged to the top, it must not land on a back button.
    val topBar = AppTheme.sizes.topBarHeight
    val margin = AppTheme.spacing.sm
    val settleSpec = AppTheme.springs.sheet.unit
    val scope = rememberCoroutineScope()

    // Read only while placing and dragging, so a moving badge is re-placed rather than recomposed.
    fun bounds(): BadgeBounds {
        val left = safe.getLeft(density, direction).toFloat()
        val right = safe.getRight(density, direction).toFloat()
        val marginPx = with(density) { margin.toPx() }
        return BadgeBounds(
            window = window,
            badge = badge,
            start = if (rtl) right else left,
            end = if (rtl) left else right,
            top = safe.getTop(density) + with(density) { topBar.toPx() } + marginPx,
            bottom = safe.getBottom(density) + with(density) { bottomChrome.value.toPx() } + marginPx,
        )
    }

    // The drag outlives recompositions (it restarts only for a new position), so it reads the
    // latest bounds, direction and settle rather than those of the composition that started it.
    val currentBounds by rememberUpdatedState(::bounds)
    val currentRtl by rememberUpdatedState(rtl)
    val currentSettleSpec by rememberUpdatedState(settleSpec)

    // Off while typing: the keyboard leaves no room for it, and it would sit on the field.
    val ime = WindowInsets.ime
    val keyboardUp by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    if (keyboardUp) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { window = it }
            .semantics { testTagsAsResourceId = true },
    ) {
        val shape = when {
            position.held -> RoundedCornerShape(percent = RoundedPercent)
            // Flush with the edge it is docked to, rounded on the side facing in.
            position.dockedAtEnd -> RoundedCornerShape(topStartPercent = RoundedPercent, bottomStartPercent = RoundedPercent)
            else -> RoundedCornerShape(topEndPercent = RoundedPercent, bottomEndPercent = RoundedPercent)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { position.offset(bounds()) }
                .onSizeChanged { badge = it }
                // Docked, the badge lies in the system's back-gesture zone, which would take a drag
                // that starts on it. Back still works along the rest of the edge.
                .systemGestureExclusion()
                .testTag(BadgeTestTag)
                .pointerInput(position) {
                    val velocity = VelocityTracker()
                    var settle: Job? = null
                    detectDragGestures(
                        onDragStart = {
                            // A finger that catches the badge mid-settle takes it over; the settle
                            // would otherwise keep writing its position under the finger.
                            settle?.cancel()
                            velocity.resetTracking()
                            position.hold(currentBounds())
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            velocity.addPointerInputChange(change)
                            position.moveBy(if (currentRtl) -drag.x else drag.x, drag.y, currentBounds())
                        },
                        onDragEnd = {
                            val flingX = velocity.calculateVelocity().x
                            val towardsEnd = if (currentRtl) -flingX else flingX
                            settle = scope.launch { position.settle(currentBounds(), towardsEnd, currentSettleSpec) }
                        },
                        onDragCancel = {
                            settle = scope.launch { position.settle(currentBounds(), 0f, currentSettleSpec) }
                        },
                    )
                }
                .graphicsLayer { alpha = if (position.held) 1f else RestAlpha }
                .pressable(onClick = onClick, feedback = PressFeedback.Scale)
                .semantics { contentDescription = "$label build. Opens the developer inspector." },
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .defaultMinSize(minWidth = AppTheme.sizes.minTouchTarget)
                    .background(AppTheme.colors.warning.subtle, shape)
                    .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
            ) {
                AppText(
                    text = label,
                    style = AppTheme.typography.caption2,
                    color = AppTheme.colors.warning.content,
                )
                pages.forEach { page ->
                    page.badgeContent?.invoke { onOpenPage(page.key) }
                }
            }
        }
    }
}

private const val RestAlpha = 0.85f
private const val RoundedPercent = 50
private const val BadgeTestTag = "devtools_badge"
