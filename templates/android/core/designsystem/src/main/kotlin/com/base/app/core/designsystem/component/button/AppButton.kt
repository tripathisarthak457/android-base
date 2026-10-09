package com.base.app.core.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.base.app.core.designsystem.component.feedback.AppActivityIndicator
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.content.ProvideContentColor
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.draw.offsetShadow
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

/**
 * How prominent an action is. A screen picks a variant by asking "how important is this action",
 * not "what colour should this be", which is what keeps two screens from disagreeing.
 */
enum class ButtonVariant {
    /** The one action the screen exists for. At most one per screen. */
    Filled,

    /** A real alternative: "Save as draft" beside "Publish". The accent, tinted. */
    Tinted,

    /** Available, but not being encouraged. Text with no container; dims under the finger. */
    Plain,

    /** Deletes or otherwise loses something. Red on a red tint, which stays readable in dark theme. */
    Destructive,
}

enum class ButtonSize { Small, Medium, Large }

/**
 * The app's button. Filled, tinted and destructive buttons shrink under the finger; a plain one
 * dims. While [loading], the label keeps its space at zero alpha and a spinner sits over it, so the
 * button never changes size.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    fillWidth: Boolean = false,
    shape: Shape = AppTheme.style.buttonShape,
) {
    val colors = AppButtonDefaults.colors(variant)
    val metrics = AppButtonDefaults.metrics(size)
    val style = AppTheme.style
    val outlined = style.offsetShadow > 0.dp && variant != ButtonVariant.Plain
    val label = if (style.uppercaseLabels) text.uppercase() else text

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .pressable(
                onClick = onClick,
                feedback = if (variant == ButtonVariant.Plain) PressFeedback.Dim else PressFeedback.Scale,
                enabled = enabled && !loading,
                role = Role.Button,
            )
            .disabledAlpha(enabled)
            .then(
                if (outlined) {
                    Modifier
                        .offsetShadow(shape, style.offsetShadow, AppTheme.colors.contentPrimary)
                        .border(BorderStroke(style.borderWidth, AppTheme.colors.contentPrimary), shape)
                } else {
                    Modifier
                },
            )
            .background(colors.container, shape)
            .heightIn(min = metrics.height)
            .padding(metrics.padding),
        contentAlignment = Alignment.Center,
    ) {
        ProvideContentColor(colors.content) {
            Row(
                modifier = if (loading) Modifier.alpha(0f) else Modifier,
                horizontalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
                AppText(
                    text = label,
                    style = if (style.uppercaseLabels) metrics.textStyle.copy(letterSpacing = UppercaseTracking) else metrics.textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                trailingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
            }
            if (loading) AppActivityIndicator(size = metrics.iconSize, color = colors.content)
        }
    }
}

/**
 * An icon-only button. A plain one is a bare glyph that dims under the finger; a tinted or filled
 * one sits in a circle and shrinks. Whatever it draws, it answers taps across 48dp.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Plain,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    tint: Color = if (variant == ButtonVariant.Plain) AppTheme.colors.contentSecondary else AppButtonDefaults.colors(variant).content,
) {
    val colors = AppButtonDefaults.colors(variant)
    val diameter = AppButtonDefaults.iconButtonSize(size)
    Box(
        modifier = modifier
            .pressable(
                onClick = onClick,
                feedback = if (variant == ButtonVariant.Plain) PressFeedback.Dim else PressFeedback.Scale,
                enabled = enabled,
                role = Role.Button,
            )
            .disabledAlpha(enabled)
            .size(diameter)
            .then(if (variant == ButtonVariant.Plain) Modifier else Modifier.background(colors.container, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, contentDescription = contentDescription, tint = tint, size = AppButtonDefaults.metrics(size).iconSize)
    }
}

/** A link: accent text that dims under the finger, for an action inside or beside running text. */
@Composable
fun AppLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = AppTheme.typography.body,
) {
    AppText(
        text = text,
        modifier = modifier
            .pressable(onClick = onClick, feedback = PressFeedback.Dim, enabled = enabled, role = Role.Button)
            .disabledAlpha(enabled),
        style = style,
        color = AppTheme.colors.accentContent,
    )
}

/** What a button looks like when the caller does not say, so a glass or branded variant can reuse it. */
object AppButtonDefaults {

    @Immutable
    class Colors(val container: Color, val content: Color)

    @Immutable
    class Metrics(
        val height: Dp,
        val padding: PaddingValues,
        val gap: Dp,
        val iconSize: Dp,
        val textStyle: TextStyle,
    )

    @Composable
    fun colors(variant: ButtonVariant): Colors {
        val colors = AppTheme.colors
        return remember(variant, colors) {
            when (variant) {
                ButtonVariant.Filled -> Colors(colors.accent, colors.onAccent)
                ButtonVariant.Tinted -> Colors(colors.accentSubtle, colors.accentContent)
                ButtonVariant.Plain -> Colors(Color.Transparent, colors.accentContent)
                ButtonVariant.Destructive -> Colors(colors.danger.subtle, colors.danger.content)
            }
        }
    }

    @Composable
    fun metrics(size: ButtonSize): Metrics {
        val sizes = AppTheme.sizes
        val typography = AppTheme.typography
        return remember(size, typography) {
            when (size) {
                ButtonSize.Small -> Metrics(
                    height = sizes.buttonSmall,
                    padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    gap = 6.dp,
                    iconSize = sizes.iconSmall,
                    textStyle = typography.subheadline.copy(fontWeight = FontWeight.SemiBold),
                )
                ButtonSize.Medium -> Metrics(
                    height = sizes.buttonMedium,
                    padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    gap = 8.dp,
                    iconSize = sizes.icon,
                    textStyle = typography.headline,
                )
                ButtonSize.Large -> Metrics(
                    height = sizes.buttonLarge,
                    padding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    gap = 10.dp,
                    iconSize = sizes.iconLarge,
                    textStyle = typography.headline,
                )
            }
        }
    }

    fun iconButtonSize(size: ButtonSize): Dp = when (size) {
        ButtonSize.Small -> 32.dp
        ButtonSize.Medium -> 40.dp
        ButtonSize.Large -> 48.dp
    }
}

/** Uppercase labels need air between the letters, or they read as a single block. */
private val UppercaseTracking = 0.08.em
