package com.base.app.core.designsystem.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A text field.
 *
 * ## The label sits above the field
 *
 * Not a floating label that animates into the border. That pattern hides the label the moment
 * there is a value — which is precisely when someone reviewing a filled-in form needs it — and
 * it is unmistakably one design language. A static label above is always readable, wraps
 * properly at large font scales, and leaves the field's own space for the value.
 *
 * ## Error text replaces helper text, and the layout does not jump
 *
 * Only one of the two is ever shown, and the row animates its height, so a validation failure
 * does not shove every field below it down the screen. Nothing about the field's own height
 * changes when it becomes invalid.
 *
 * ## The length cap is enforced here
 *
 * [maxLength] rejects the excess character instead of accepting it and showing a counter in red.
 * A field that lets you type past the limit and then refuses to submit is a field that wasted
 * your time twice.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    maxLength: Int? = null,
    showCounter: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val focused by interactionSource.collectIsFocusedAsState()
    val hasError = error != null

    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> colors.danger.content
            focused -> colors.accent
            else -> colors.border
        },
        animationSpec = tween(AppTheme.motion.quick),
        label = "fieldBorder",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (focused || hasError) AppTheme.sizes.borderWidthStrong else AppTheme.sizes.borderWidth,
        animationSpec = tween(AppTheme.motion.quick),
        label = "fieldBorderWidth",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        label?.let {
            AppText(
                text = it,
                style = AppTheme.typography.titleSmall,
                color = if (enabled) colors.contentSecondary else colors.contentDisabled,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .disabledAlpha(enabled)
                .background(
                    color = if (enabled) colors.surface else colors.surfaceVariant,
                    shape = AppTheme.shapes.sm,
                )
                .border(borderWidth, borderColor, AppTheme.shapes.sm)
                .padding(horizontal = spacing.md)
                .defaultMinSize(minHeight = AppTheme.sizes.fieldHeight),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let {
                AppIcon(it, contentDescription = null, tint = colors.contentTertiary)
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty() && placeholder != null) {
                    AppText(
                        text = placeholder,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.contentTertiary,
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = { candidate ->
                        if (maxLength == null || candidate.length <= maxLength) onValueChange(candidate)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = spacing.md)
                        .semantics { if (error != null) this.error(error) },
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = AppTheme.typography.bodyMedium.copy(color = colors.contentPrimary),
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                    visualTransformation = visualTransformation,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(if (hasError) colors.danger.content else colors.accent),
                )
            }

            trailing?.invoke()
        }

        SupportingRow(
            message = error ?: helper,
            isError = hasError,
            counter = if (showCounter && maxLength != null) "${value.length}/$maxLength" else null,
        )
    }
}

@Composable
private fun SupportingRow(
    message: String?,
    isError: Boolean,
    counter: String?,
) {
    val colors = AppTheme.colors

    AnimatedVisibility(
        visible = message != null || counter != null,
        enter = fadeIn(tween(AppTheme.motion.quick)) + expandVertically(tween(AppTheme.motion.quick)),
        exit = fadeOut(tween(AppTheme.motion.instant)) + shrinkVertically(tween(AppTheme.motion.instant)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            AppText(
                text = message.orEmpty(),
                modifier = Modifier.weight(1f, fill = false),
                style = AppTheme.typography.caption,
                color = if (isError) colors.danger.content else colors.contentTertiary,
            )
            counter?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.caption,
                    color = colors.contentTertiary,
                )
            }
        }
    }
}
