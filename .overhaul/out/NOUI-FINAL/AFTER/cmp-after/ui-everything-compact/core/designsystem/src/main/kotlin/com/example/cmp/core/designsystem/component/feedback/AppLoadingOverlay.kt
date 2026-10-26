package com.example.cmp.core.designsystem.component.feedback

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.component.overlay.HostedOverlay
import com.example.cmp.core.designsystem.component.overlay.OverlayBackHandler
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.content.AppSurface
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme

/**
 * A dimmed window with a spinner while something irreversible is in flight: a payment, a sign-up.
 * It renders above everything, the tab bar included, and while it is up nothing beneath can be
 * touched, focused by a screen reader, or left with Back. Keep it composed and drive [visible].
 */
@Composable
public fun AppLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return
    // Taken and ignored: leaving the screen would not stop what is in flight.
    OverlayBackHandler(enabled = state.targetState) {}

    HostedOverlay(modal = { true }) {
        val springs = AppTheme.springs
        val scrim = AppTheme.colors.scrim
        val transition = rememberTransition(state, label = "loadingOverlay")
        val shown = transition.animateFloat(
            transitionSpec = { springs.dialog.unit },
            label = "loadingShown",
        ) { if (it) 1f else 0f }

        Box(
            modifier = modifier
                .fillMaxSize()
                .drawBehind { drawRect(scrim, alpha = shown.value.coerceIn(0f, 1f)) }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach {
                                it.consume()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AppSurface(
                modifier = Modifier.graphicsLayer { alpha = shown.value.coerceIn(0f, 1f) },
                shape = AppTheme.shapes.lg,
                color = AppTheme.colors.surfaceElevated,
                elevation = AppTheme.elevation.modal,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = AppLoadingOverlayDefaults.MaxPanelWidth)
                        .padding(AppTheme.spacing.xl)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppActivityIndicator(size = AppLoadingOverlayDefaults.IndicatorSize)
                    label?.let {
                        AppText(
                            text = it,
                            style = AppTheme.typography.subheadline,
                            color = AppTheme.colors.contentSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** The loading overlay's panel. */
public object AppLoadingOverlayDefaults {
    public val MaxPanelWidth: Dp = 240.dp
    public val IndicatorSize: Dp = 28.dp
}

@AppComponentPreviews
@Composable
private fun AppLoadingOverlayPreview() {
    Box(
        modifier = Modifier
            .background(AppTheme.colors.background)
            .fillMaxWidth()
            .height(AppTheme.sizes.rowHeight * 5),
    ) {
        AppText(text = "Checkout", modifier = Modifier.padding(AppTheme.spacing.lg))
        AppLoadingOverlay(visible = true, label = "Placing your order")
    }
}
