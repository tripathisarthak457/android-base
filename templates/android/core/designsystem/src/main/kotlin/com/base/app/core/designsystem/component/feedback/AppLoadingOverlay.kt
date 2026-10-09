package com.base.app.core.designsystem.component.feedback

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.overlay.HostedOverlay
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.AppSurface
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A dimmed window with a spinner while something irreversible is in flight: a payment, a sign-up.
 * It renders above everything, the tab bar included, and while it is up nothing beneath can be
 * touched, focused by a screen reader, or left with Back. Keep it composed and drive [visible].
 */
@Composable
fun AppLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val state = remember { MutableTransitionState(false) }
    state.targetState = visible
    if (!state.currentState && !state.targetState) return

    HostedOverlay(modal = true) {
        val springs = AppTheme.springs
        val transition = rememberTransition(state, label = "loadingOverlay")
        val shown = transition.animateFloat(transitionSpec = { springs.sheet.unit }, label = "loadingShown") { if (it) 1f else 0f }
        BackHandler(enabled = state.targetState) {}

        Box(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer { alpha = shown.value.coerceIn(0f, 1f) }
                .background(AppTheme.colors.scrim)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AppSurface(
                shape = AppTheme.shapes.lg,
                color = AppTheme.colors.surfaceElevated,
                elevation = AppTheme.elevation.modal,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .padding(AppTheme.spacing.xl)
                        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppActivityIndicator(size = 28.dp)
                    label?.let {
                        AppText(text = it, style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
