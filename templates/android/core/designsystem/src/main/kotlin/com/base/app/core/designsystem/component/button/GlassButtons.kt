package com.base.app.core.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.foundation.draw.LocalRedacted
import com.base.app.core.designsystem.foundation.draw.skeleton
import com.base.app.core.designsystem.foundation.glass.GlassDefaults
import com.base.app.core.designsystem.foundation.glass.glassSurface
import com.base.app.core.designsystem.foundation.glass.liquidPress
import com.base.app.core.designsystem.foundation.glass.pressGlow
import com.base.app.core.designsystem.foundation.glass.rememberLiquidPress
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Whether plain icon buttons here are drawn as glass discs: a top bar under the Glass design
 * style says so for its back button and its actions, as iOS 26 draws them, while a plain icon
 * button in a row or a card stays a bare glyph.
 */
internal val LocalGlassButtons = staticCompositionLocalOf { false }

/**
 * [AppButton] under the Glass design style: a capsule of drawn glass that swells, slides and
 * glows under the finger ([liquidPress]), as the Kyant catalog's `LiquidButton` does. A filled
 * button is the glass tinted with the accent, as the catalog tints its buttons; a tinted or
 * destructive one is clear glass with a coloured label. Glass inside a screen samples nothing: a
 * surface inside the recorded scene would sample a recording of itself.
 */
@Composable
internal fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    variant: ButtonVariant,
    size: ButtonSize,
    enabled: Boolean,
    loading: Boolean,
    leadingIcon: ImageVector?,
    trailingIcon: ImageVector?,
    shape: Shape,
    colors: ButtonColors,
) {
    val metrics = AppButtonDefaults.metrics(size)
    val source = remember { MutableInteractionSource() }
    val press = rememberLiquidPress(source)
    val tint = if (variant == ButtonVariant.Filled) colors.container(enabled).copy(alpha = FilledTintAlpha) else Color.Transparent
    val content = if (variant == ButtonVariant.Filled || !enabled) colors.content(enabled) else colors.container(enabled)
    val redacted = if (LocalRedacted.current) Modifier.skeleton(shape) else Modifier
    Box(
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.None, enabled = enabled && !loading, role = Role.Button, interactionSource = source)
            .then(redacted)
            .liquidPress(press)
            .glassSurface(shape, GlassDefaults.contentColors(), GlassDefaults.contentShadow())
            .then(if (tint.alpha > 0f) Modifier.background(tint, shape) else Modifier)
            .pressGlow({ press.strength }, { press.touch }, shape)
            .heightIn(min = metrics.height)
            .padding(metrics.padding),
        contentAlignment = Alignment.Center,
    ) {
        ButtonContent(text, content, metrics, loading, leadingIcon, trailingIcon)
    }
}

/**
 * A plain [AppIconButton] in a top bar under the Glass design style: a disc of drawn glass over
 * the content, with the glyph in the bar's accent, that swells and glows under the finger.
 */
@Composable
internal fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    size: ButtonSize,
    enabled: Boolean,
    colors: ButtonColors,
) {
    val diameter = AppButtonDefaults.iconButtonSize(size)
    val source = remember { MutableInteractionSource() }
    val press = rememberLiquidPress(source)
    val redacted = if (LocalRedacted.current) Modifier.skeleton(CircleShape) else Modifier
    Box(
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.None, enabled = enabled, role = Role.Button, interactionSource = source)
            .size(diameter)
            .then(redacted)
            .liquidPress(press)
            .glassSurface(CircleShape, GlassDefaults.contentColors(), GlassDefaults.contentShadow())
            .pressGlow({ press.strength }, { press.touch }, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        val content = if (enabled) AppTheme.colors.accentContent else colors.content(enabled)
        AppIcon(icon, contentDescription = contentDescription, tint = content, size = AppButtonDefaults.metrics(size).iconSize)
    }
}

/** The catalog tints a glass button with its colour at three quarters, so the glass still shows. */
private const val FilledTintAlpha = 0.75f
