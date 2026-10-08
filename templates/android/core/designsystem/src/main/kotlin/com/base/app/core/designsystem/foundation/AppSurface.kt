package com.base.app.core.designsystem.foundation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The one container every other component is built on: a shape, a fill, an optional outline, an
 * optional lift, and a content colour for everything inside it.
 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.contentPrimary,
    border: BorderStroke? = null,
    elevation: Dp = AppTheme.elevation.none,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val lifted = elevation > 0.dp
    val outline = border ?: if (!colors.isLight && lifted) {
        BorderStroke(AppTheme.sizes.borderWidth, colors.border)
    } else {
        border
    }

    Box(
        modifier = modifier
            .shadowIf(colors.isLight && lifted, elevation, shape)
            .clip(shape)
            .background(color = color, shape = shape)
            .borderIf(outline, shape),
        contentAlignment = contentAlignment,
    ) {
        ProvideContentColor(contentColor) { content() }
    }
}

/**
 * A surface that responds to a tap. [contentAlignment] matters whenever a minimum size makes the
 * surface larger than what is inside it — an icon button, a chip.
 */
@Composable
fun AppClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AppTheme.shapes.md,
    color: Color = AppTheme.colors.surface,
    contentColor: Color = AppTheme.colors.contentPrimary,
    border: BorderStroke? = null,
    elevation: Dp = AppTheme.elevation.none,
    role: Role? = Role.Button,
    scaleOnPress: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit,
) {
    val colors = AppTheme.colors
    val lifted = elevation > 0.dp
    val outline = border ?: if (!colors.isLight && lifted) {
        BorderStroke(AppTheme.sizes.borderWidth, colors.border)
    } else {
        border
    }

    Box(
        // `disabledAlpha` before the background, or the container stays at full strength when
        // disabled.
        modifier = modifier
            .disabledAlpha(enabled)
            .then(if (scaleOnPress) Modifier.pressScale(interactionSource, enabled) else Modifier)
            .shadowIf(colors.isLight && lifted, elevation, shape)
            .clip(shape)
            .background(color = color, shape = shape)
            .borderIf(outline, shape)
            .appClickable(
                onClick = onClick,
                enabled = enabled,
                role = role,
                interactionSource = interactionSource,
                minTouchTarget = 0.dp,
            ),
        contentAlignment = contentAlignment,
    ) {
        ProvideContentColor(contentColor) { content() }
    }
}

/*
 * The shadow is tinted rather than pure black.
 */
private val ShadowTint = Color(0xFF0B0F1A)

private fun Modifier.shadowIf(condition: Boolean, elevation: Dp, shape: Shape): Modifier {
    if (!condition) return this
    return shadow(
        elevation = elevation,
        shape = shape,
        clip = false,
        ambientColor = ShadowTint,
        spotColor = ShadowTint,
    )
}

private fun Modifier.borderIf(stroke: BorderStroke?, shape: Shape): Modifier =
    if (stroke == null) this else border(stroke, shape)
