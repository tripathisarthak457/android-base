package com.base.app.core.designsystem.component.button

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.feedback.AppCircularProgress
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.AppClickableSurface
import com.base.app.core.designsystem.foundation.ProvideContentColor
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The five things a button can mean. Not five ways it can look — the distinction matters, because
 * a screen picks a variant by asking "how important is this action" rather than "what colour
 * should this be", and that is what keeps two screens from disagreeing.
 */
enum class ButtonVariant {
    /** The one action the screen exists for. At most one per screen. */
    Primary,

    /** A real alternative to Primary — "Save as draft" beside "Publish". */
    Secondary,

    /** Available, but not being encouraged. Text with no container. */
    Tertiary,

    /** Deletes, cancels, or otherwise loses something. */
    Destructive,

    /** For a toolbar or a dense row, where a filled container would be noise. */
    Ghost,
}

enum class ButtonSize { Small, Medium, Large }

/**
 * The app's button.
 *
 * ## Loading does not resize the button
 *
 * While [loading], the label stays laid out at zero alpha and a spinner is centred over it. The
 * obvious implementation — swap the label for a spinner — makes the button shrink to spinner
 * width the instant it is tapped, which shifts everything beside it and, on a full-width submit,
 * looks like the screen broke. Keeping the label's footprint costs one `Box` and removes the
 * whole problem.
 *
 * A loading button is also not clickable. Leaving it enabled is how a double tap submits an order
 * twice.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    fillWidth: Boolean = false,
    shape: Shape = AppTheme.shapes.sm,
) {
    val style = variant.style()
    val metrics = size.metrics()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val container by animateColorAsState(
        targetValue = if (pressed) style.containerPressed else style.container,
        animationSpec = androidx.compose.animation.core.tween(AppTheme.motion.instant),
        label = "buttonContainer",
    )

    AppClickableSurface(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = metrics.height),
        enabled = enabled && !loading,
        shape = shape,
        color = container,
        contentColor = style.content,
        border = style.border,
        interactionSource = interactionSource,
        role = Role.Button,
        // The surface is at least `metrics.height` tall and, when filled, the whole width — both
        // larger than the label. Without this the label is laid out in the top-left corner of its
        // own button, which is the single most visible way a hand-built button gives itself away.
        contentAlignment = Alignment.Center,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .padding(metrics.padding)
                    .alpha(if (loading) 0f else 1f),
                horizontalArrangement = Arrangement.spacedBy(metrics.gap, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                leadingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
                AppText(
                    text = text,
                    style = metrics.textStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                trailingIcon?.let { AppIcon(it, contentDescription = null, size = metrics.iconSize) }
            }

            if (loading) {
                ProvideContentColor(style.content) {
                    AppCircularProgress(size = metrics.iconSize, strokeWidth = 2.dp)
                }
            }
        }
    }
}

/**
 * An icon-only button.
 *
 * [contentDescription] is required rather than nullable: with no label, it is the only thing a
 * screen reader can announce, and an optional parameter here is one that gets omitted.
 */
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Ghost,
    size: ButtonSize = ButtonSize.Medium,
    enabled: Boolean = true,
    shape: Shape = AppTheme.shapes.sm,
) {
    val style = variant.style()
    val metrics = size.metrics()

    AppClickableSurface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minWidth = metrics.height, minHeight = metrics.height),
        enabled = enabled,
        shape = shape,
        color = style.container,
        contentColor = style.content,
        border = style.border,
        role = Role.Button,
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, contentDescription = contentDescription, size = metrics.iconSize)
    }
}

@Immutable
private data class ButtonStyle(
    val container: Color,
    val containerPressed: Color,
    val content: Color,
    val border: BorderStroke?,
)

@Composable
private fun ButtonVariant.style(): ButtonStyle {
    val colors = AppTheme.colors
    val width = AppTheme.sizes.borderWidth
    return when (this) {
        ButtonVariant.Primary -> ButtonStyle(
            container = colors.accent,
            containerPressed = colors.accentPressed,
            content = colors.onAccent,
            border = null,
        )

        ButtonVariant.Secondary -> ButtonStyle(
            container = colors.surface,
            containerPressed = colors.surfaceVariant,
            content = colors.contentPrimary,
            border = BorderStroke(width, colors.borderStrong),
        )

        ButtonVariant.Tertiary -> ButtonStyle(
            container = Color.Transparent,
            containerPressed = colors.accentSubtle,
            content = colors.accent,
            border = null,
        )

        ButtonVariant.Destructive -> ButtonStyle(
            container = colors.danger.content,
            containerPressed = colors.danger.content.copy(alpha = 0.86f),
            content = Color.White,
            border = null,
        )

        ButtonVariant.Ghost -> ButtonStyle(
            container = Color.Transparent,
            containerPressed = colors.surfaceVariant,
            content = colors.contentSecondary,
            border = null,
        )
    }
}

@Immutable
private data class ButtonMetrics(
    val height: Dp,
    val padding: PaddingValues,
    val gap: Dp,
    val iconSize: Dp,
    val textStyle: TextStyle,
)

@Composable
private fun ButtonSize.metrics(): ButtonMetrics {
    val sizes = AppTheme.sizes
    val typography = AppTheme.typography
    return when (this) {
        ButtonSize.Small -> ButtonMetrics(
            height = sizes.buttonSmall,
            padding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            gap = 6.dp,
            iconSize = sizes.iconSmall,
            textStyle = typography.titleSmall,
        )

        ButtonSize.Medium -> ButtonMetrics(
            height = sizes.buttonMedium,
            padding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            gap = 8.dp,
            iconSize = sizes.icon,
            textStyle = typography.button,
        )

        ButtonSize.Large -> ButtonMetrics(
            height = sizes.buttonLarge,
            padding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            gap = 10.dp,
            iconSize = sizes.iconLarge,
            textStyle = typography.button,
        )
    }
}
