package com.base.app.core.designsystem.component.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.theme.AppTheme

enum class ButtonVariant {
    /** The one action the screen exists for. At most one per screen. */
    Filled,

    /** A real alternative: "Save as draft" beside "Publish". The accent, tinted. */
    Tinted,

    /** Available, but not being encouraged. Text with no container; dims under the finger. */
    Plain,

    /** Deletes or otherwise loses something. Red on a red tint, readable in both themes. */
    Destructive,
}

enum class ButtonSize { Small, Medium, Large }

/**
 * The app's button. Filled, tinted and destructive buttons shrink under the finger; a plain one
 * dims. A disabled button is drawn in grey rather than faded, so its label stays legible. For a
 * button as wide as its column, pass `Modifier.fillMaxWidth()`.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Filled,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    shape: Shape = AppTheme.style.buttonShape,
    colors: ButtonColors = AppButtonDefaults.colors(variant),
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = AppButtonDefaults.height(size))
            .pressable(
                onClick = onClick,
                feedback = if (variant == ButtonVariant.Plain) {
                    PressFeedback.Dim
                } else {
                    PressFeedback.Scale
                },
                enabled = enabled,
            )
            .background(colors.container(enabled), shape)
            .padding(horizontal = AppButtonDefaults.horizontalPadding(size)),
        contentAlignment = Alignment.Center,
    ) {
        AppText(
            text = text,
            color = colors.content(enabled),
            style = if (size == ButtonSize.Small) {
                AppTheme.typography.subheadline
            } else {
                AppTheme.typography.headline
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A button's colours in each state. */
@Immutable
class ButtonColors(
    private val containerColor: Color,
    private val contentColor: Color,
    private val disabledContainerColor: Color,
    private val disabledContentColor: Color,
) {
    internal fun container(enabled: Boolean): Color =
        if (enabled) containerColor else disabledContainerColor

    internal fun content(enabled: Boolean): Color = if (enabled) contentColor else disabledContentColor
}

object AppButtonDefaults {

    @Composable
    fun colors(variant: ButtonVariant): ButtonColors {
        val colors = AppTheme.colors
        return when (variant) {
            ButtonVariant.Filled ->
                ButtonColors(colors.accent, colors.onAccent, colors.fill, colors.contentDisabled)
            ButtonVariant.Tinted ->
                ButtonColors(colors.accentSubtle, colors.accentContent, colors.fill, colors.contentDisabled)
            ButtonVariant.Plain ->
                ButtonColors(Color.Transparent, colors.accentContent, Color.Transparent, colors.contentDisabled)
            ButtonVariant.Destructive ->
                ButtonColors(colors.dangerSubtle, colors.danger, colors.fill, colors.contentDisabled)
        }
    }

    @Composable
    internal fun height(size: ButtonSize): Dp = when (size) {
        ButtonSize.Small -> AppTheme.sizes.buttonSmall
        ButtonSize.Medium -> AppTheme.sizes.buttonMedium
        ButtonSize.Large -> AppTheme.sizes.buttonLarge
    }

    @Composable
    internal fun horizontalPadding(size: ButtonSize): Dp = when (size) {
        ButtonSize.Small -> AppTheme.spacing.md
        ButtonSize.Medium -> AppTheme.spacing.lg
        ButtonSize.Large -> AppTheme.spacing.xl
    }
}
