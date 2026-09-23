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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.FieldTreatment

/** A text field. Not a floating label that animates into the border. */
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
    val treatment = AppTheme.style.field

    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> colors.danger.content
            focused -> colors.accent
            treatment == FieldTreatment.Filled -> Color.Transparent
            treatment == FieldTreatment.Underlined -> colors.borderStrong
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
                .fieldContainer(treatment, enabled, borderWidth, borderColor)
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

/** The box around the value, per [FieldTreatment]. The label and supporting row never change. */
@Composable
private fun Modifier.fieldContainer(
    treatment: FieldTreatment,
    enabled: Boolean,
    borderWidth: Dp,
    borderColor: Color,
): Modifier {
    val colors = AppTheme.colors
    val shape = AppTheme.shapes.sm
    return when (treatment) {
        FieldTreatment.Outlined -> background(if (enabled) colors.surface else colors.surfaceVariant, shape)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = AppTheme.spacing.md)

        FieldTreatment.Filled -> background(colors.surfaceVariant, shape)
            .border(borderWidth, borderColor, shape)
            .padding(horizontal = AppTheme.spacing.md)

        FieldTreatment.Underlined -> drawBehind {
            val stroke = borderWidth.toPx()
            val y = size.height - stroke / 2
            drawLine(borderColor, Offset(0f, y), Offset(size.width, y), stroke)
        }.padding(horizontal = AppTheme.spacing.xxs)
    }
}
