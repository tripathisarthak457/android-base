package com.example.lean.core.designsystem.component.button

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.lean.core.designsystem.component.feedback.AppActivityIndicator
import com.example.lean.core.designsystem.component.text.AppIcon
import com.example.lean.core.designsystem.component.text.AppText
import com.example.lean.core.designsystem.component.text.scaledLike
import com.example.lean.core.designsystem.foundation.content.ProvideContentColor
import com.example.lean.core.designsystem.foundation.draw.ContentUnderBone
import com.example.lean.core.designsystem.foundation.draw.LocalRedacted
import com.example.lean.core.designsystem.foundation.draw.offsetShadow
import com.example.lean.core.designsystem.foundation.draw.skeleton
import com.example.lean.core.designsystem.foundation.interaction.PressFeedback
import com.example.lean.core.designsystem.foundation.interaction.inlinePressable
import com.example.lean.core.designsystem.foundation.interaction.pressable
import com.example.lean.core.designsystem.theme.AppTheme

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
 * button never changes size. A disabled button is drawn in grey rather than faded, so its label
 * stays legible. For a button as wide as its column, pass `Modifier.fillMaxWidth()`.
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
    shape: Shape = AppTheme.style.buttonShape,
    colors: ButtonColors = AppButtonDefaults.colors(variant),
) {
    val metrics = AppButtonDefaults.metrics(size)
    val style = AppTheme.style
    val outlined = style.offsetShadow > 0.dp && variant != ButtonVariant.Plain
    val label = if (style.uppercaseLabels) text.uppercase() else text

    val content = colors.content(enabled)
    // A plain button has no container to stand for it: its label and icons redact themselves.
    val bone = LocalRedacted.current && variant != ButtonVariant.Plain
    val redacted = if (bone) Modifier.skeleton(shape) else Modifier
    Box(
        modifier = modifier
            .pressable(
                onClick = onClick,
                feedback = if (variant == ButtonVariant.Plain) PressFeedback.Dim else PressFeedback.Scale,
                enabled = enabled && !loading,
                role = Role.Button,
            )
            .then(redacted)
            .then(
                if (outlined) {
                    Modifier
                        .offsetShadow(shape, style.offsetShadow, AppTheme.colors.contentPrimary)
                        .border(BorderStroke(style.borderWidth, AppTheme.colors.contentPrimary), shape)
                } else {
                    Modifier
                },
            )
            .background(colors.container(enabled), shape)
            .heightIn(min = metrics.height)
            .padding(metrics.padding),
        contentAlignment = Alignment.Center,
    ) {
        ContentUnderBone(bone) {
            ProvideContentColor(content) {
                Row(
                    modifier = if (loading) Modifier.alpha(0f) else Modifier,
                    horizontalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    leadingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
                    // A label wider than the button wraps rather than losing its end, so at twice the
                    // text size a long label still reads whole.
                    AppText(
                        text = label,
                        style = if (style.uppercaseLabels) {
                            metrics.textStyle.copy(letterSpacing = UppercaseTracking)
                        } else {
                            metrics.textStyle
                        },
                        textAlign = TextAlign.Center,
                    )
                    trailingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
                }
                if (loading) AppActivityIndicator(size = metrics.iconSize, color = content)
            }
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
    colors: ButtonColors = AppButtonDefaults.iconButtonColors(variant),
) {
    IconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        variant = variant,
        diameter = AppButtonDefaults.iconButtonSize(size),
        glyphSize = AppButtonDefaults.metrics(size).iconSize,
        enabled = enabled,
        colors = colors,
    )
}

/**
 * A plain icon button inside a field, beside text in [textStyle]: it grows with that text at the
 * person's font scale, as the field's leading glyph does, rather than staying a small mark at the
 * end of a line twice its size. Bar buttons keep a fixed size, which is why [AppIconButton] does not.
 */
@Composable
internal fun FieldIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        variant = ButtonVariant.Plain,
        diameter = AppButtonDefaults.iconButtonSize(ButtonSize.Small).scaledLike(textStyle),
        glyphSize = AppButtonDefaults.metrics(ButtonSize.Small).iconSize.scaledLike(textStyle),
        enabled = enabled,
        colors = AppButtonDefaults.iconButtonColors(ButtonVariant.Plain),
    )
}

@Composable
private fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    variant: ButtonVariant,
    diameter: Dp,
    glyphSize: Dp,
    enabled: Boolean,
    colors: ButtonColors,
) {
    val container = colors.container(enabled)
    val bone = LocalRedacted.current && container.alpha > 0f
    val redacted = if (bone) Modifier.skeleton(CircleShape) else Modifier
    Box(
        modifier = modifier
            .pressable(
                onClick = onClick,
                feedback = if (variant == ButtonVariant.Plain) PressFeedback.Dim else PressFeedback.Scale,
                enabled = enabled,
                role = Role.Button,
            )
            .size(diameter)
            .then(redacted)
            .then(if (container.alpha == 0f) Modifier else Modifier.background(container, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        ContentUnderBone(bone) {
            AppIcon(
                icon,
                contentDescription = contentDescription,
                tint = colors.content(enabled),
                size = glyphSize,
            )
        }
    }
}

/**
 * A link: accent text that dims under the finger, for an action inside or beside running text. It is
 * as wide as its label, so it starts on the text column around it; touch still reaches 48dp across.
 */
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
        modifier = modifier.inlinePressable(onClick = onClick, feedback = PressFeedback.Dim, enabled = enabled),
        style = style,
        color = if (enabled) AppTheme.colors.accentContent else AppTheme.colors.contentDisabled,
    )
}

/**
 * A button's colours, enabled and disabled. Build one from [AppButtonDefaults.colors] to change a
 * single button; change the theme's roles to change them all.
 */
@Immutable
class ButtonColors(
    val container: Color,
    val content: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
) {
    fun copy(
        container: Color = this.container,
        content: Color = this.content,
        disabledContainer: Color = this.disabledContainer,
        disabledContent: Color = this.disabledContent,
    ) = ButtonColors(container, content, disabledContainer, disabledContent)

    internal fun container(enabled: Boolean) = if (enabled) container else disabledContainer

    internal fun content(enabled: Boolean) = if (enabled) content else disabledContent

    override fun equals(other: Any?): Boolean = other is ButtonColors &&
        container == other.container && content == other.content &&
        disabledContainer == other.disabledContainer && disabledContent == other.disabledContent

    override fun hashCode(): Int {
        var result = container.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + disabledContainer.hashCode()
        return 31 * result + disabledContent.hashCode()
    }
}

/** What a button looks like when the caller does not say, so a glass or branded variant can reuse it. */
object AppButtonDefaults {

    @Immutable
    class Metrics(
        val height: Dp,
        val padding: PaddingValues,
        val gap: Dp,
        val iconSize: Dp,
        val textStyle: TextStyle,
    )

    /** A [variant]'s colours. Disabled, every variant goes grey, so a disabled label never fades. */
    @Composable
    fun colors(variant: ButtonVariant): ButtonColors {
        val colors = AppTheme.colors
        return remember(variant, colors) {
            val disabledContainer = if (variant == ButtonVariant.Plain) Color.Transparent else colors.fill
            when (variant) {
                ButtonVariant.Filled -> ButtonColors(colors.accent, colors.onAccent, disabledContainer, colors.contentDisabled)
                ButtonVariant.Tinted -> ButtonColors(colors.accentSubtle, colors.accentContent, disabledContainer, colors.contentDisabled)
                ButtonVariant.Plain -> ButtonColors(Color.Transparent, colors.accentContent, disabledContainer, colors.contentDisabled)
                ButtonVariant.Destructive -> ButtonColors(colors.danger.subtle, colors.danger.content, disabledContainer, colors.contentDisabled)
            }
        }
    }

    /**
     * An icon button's colours: as [colors], except that a plain glyph is secondary text rather than
     * the accent. [content] overrides the glyph's colour when it is specified.
     */
    @Composable
    fun iconButtonColors(variant: ButtonVariant = ButtonVariant.Plain, content: Color = Color.Unspecified): ButtonColors {
        val colors = colors(variant)
        val glyph = when {
            content.isSpecified -> content
            variant == ButtonVariant.Plain -> AppTheme.colors.contentSecondary
            else -> return colors
        }
        return remember(colors, glyph) { colors.copy(content = glyph) }
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

private const val H8_PROBE = 1
