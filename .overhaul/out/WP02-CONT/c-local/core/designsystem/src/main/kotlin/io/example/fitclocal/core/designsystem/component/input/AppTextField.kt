package io.example.fitclocal.core.designsystem.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import io.example.fitclocal.core.designsystem.component.text.AppIcon
import io.example.fitclocal.core.designsystem.component.text.AppText
import io.example.fitclocal.core.designsystem.component.text.scaledLike
import io.example.fitclocal.core.designsystem.foundation.draw.disabledAlpha
import io.example.fitclocal.core.designsystem.theme.AppTheme
import io.example.fitclocal.core.designsystem.theme.FieldTreatment
import kotlin.math.min

/**
 * A text field over a [TextFieldState] the ViewModel owns, so the text survives process death and is
 * never copied back and forth through UI state (which is what makes a caret jump).
 *
 * The label, placeholder and supporting line are part of the field, so a screen reader reads them
 * with it and a tap anywhere on them focuses it. [prefix] and [suffix] sit beside the text ("+1",
 * "kg"); an [error] tints the box and replaces the [helper]. [maxLength] refuses input past the
 * limit, a paste included.
 *
 * [onSubmit] is what Enter on a hardware keyboard does, as on a desktop: send the message, sign in.
 * In a multi-line field Shift+Enter still starts a new line. On-screen keyboards keep their own
 * action key, which [onKeyboardAction] handles.
 */
@Composable
fun AppTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    maxLength: Int? = null,
    showCounter: Boolean = false,
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    onSubmit: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val submit by rememberUpdatedState(onSubmit)
    val hardwareKeyboard = AppTheme.input.hardwareKeyboard
    val focused by source.collectIsFocusedAsState()
    val limit = maxLength?.let { InputTransformation.maxLength(it) }
    val transformation = when {
        limit == null -> inputTransformation
        inputTransformation == null -> limit
        else -> inputTransformation.then(limit)
    }
    val colors = AppTheme.colors

    BasicTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onSubmit != null && hardwareKeyboard) {
                    Modifier.onPreviewKeyEvent { event ->
                        val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                        if (!enter || event.isShiftPressed || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        submit?.invoke()
                        true
                    }
                } else {
                    Modifier
                },
            )
            .disabledAlpha(enabled)
            .semantics { if (error != null) error(error) },
        enabled = enabled,
        readOnly = readOnly,
        inputTransformation = transformation,
        outputTransformation = outputTransformation,
        textStyle = AppTheme.typography.body.copy(color = colors.contentPrimary),
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        lineLimits = lineLimits,
        interactionSource = source,
        cursorBrush = SolidColor(if (error != null) colors.danger.content else colors.accent),
        decorator = fieldDecorator(
            empty = { state.text.isEmpty() },
            focused = focused,
            label = label,
            placeholder = placeholder,
            supporting = error ?: helper,
            isError = error != null,
            counter = if (showCounter && maxLength != null) "${state.text.length}/$maxLength" else null,
            prefix = prefix,
            suffix = suffix,
            leadingIcon = leadingIcon,
            trailing = trailing,
            multiline = lineLimits != TextFieldLineLimits.SingleLine,
        ),
    )
}

/**
 * The box around a field's text, shared by every field in the design system. Colours that change
 * with focus and error are read while drawing.
 */
@Composable
internal fun fieldDecorator(
    empty: () -> Boolean,
    focused: Boolean,
    label: String?,
    placeholder: String?,
    supporting: String?,
    isError: Boolean,
    counter: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    textStyle: TextStyle = AppTheme.typography.body,
    multiline: Boolean = false,
): TextFieldDecorator {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val treatment = AppTheme.style.field
    val shape = AppTheme.shapes.sm
    val strokeWidth = if (focused || isError) AppTheme.style.borderWidthStrong else AppTheme.style.borderWidth
    val edge = animateColorAsState(
        targetValue = when {
            isError -> colors.danger.content
            focused -> colors.accent
            treatment == FieldTreatment.Filled -> Color.Transparent
            else -> colors.outline
        },
        animationSpec = tween(AppTheme.springs.fadeQuick),
        label = "fieldEdge",
    )
    val fill = when {
        isError && treatment != FieldTreatment.Underlined -> colors.danger.subtle
        treatment == FieldTreatment.Filled -> colors.fill
        treatment == FieldTreatment.Outlined -> colors.surface
        else -> Color.Transparent
    }

    return TextFieldDecorator { innerTextField ->
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            label?.let {
                AppText(text = it, style = AppTheme.typography.footnote, color = colors.contentSecondary)
            }
            Row(
                modifier = Modifier
                    .fieldBoxTop()
                    .fillMaxWidth()
                    .heightIn(min = AppTheme.sizes.fieldHeight)
                    .fieldBox(treatment, shape, fill, edge, strokeWidth.value)
                    .padding(horizontal = if (treatment == FieldTreatment.Underlined) spacing.xxs else spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                // A multi-line field grows downwards from its first line, so everything beside the
                // text lines up with that line rather than with the middle of the box.
                verticalAlignment = if (multiline) Alignment.Top else Alignment.CenterVertically,
            ) {
                val besideText = if (multiline) Modifier.padding(top = spacing.md) else Modifier
                leadingIcon?.let {
                    AppIcon(
                        it,
                        contentDescription = null,
                        modifier = besideText,
                        tint = colors.contentTertiary,
                        size = AppTheme.sizes.icon.scaledLike(textStyle),
                    )
                }
                prefix?.let { AppText(text = it, modifier = besideText, style = textStyle, color = colors.contentSecondary) }
                Box(
                    modifier = Modifier.weight(1f).padding(vertical = spacing.md),
                    contentAlignment = if (multiline) Alignment.TopStart else Alignment.CenterStart,
                ) {
                    if (placeholder != null && empty()) {
                        AppText(
                            text = placeholder,
                            style = textStyle,
                            color = colors.contentTertiary,
                            maxLines = if (multiline) Int.MAX_VALUE else 1,
                        )
                    }
                    innerTextField()
                }
                suffix?.let { AppText(text = it, modifier = besideText, style = textStyle, color = colors.contentSecondary) }
                trailing?.let { Box(besideText) { it() } }
            }
            AnimatedVisibility(visible = supporting != null || counter != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(
                        text = supporting.orEmpty(),
                        modifier = Modifier.weight(1f, fill = false),
                        style = AppTheme.typography.footnote,
                        color = if (isError) colors.danger.content else colors.contentSecondary,
                    )
                    counter?.let { AppText(text = it, style = AppTheme.typography.footnote, color = colors.contentSecondary) }
                }
            }
        }
    }
}

/**
 * The top edge of a field's box, below its label. Fields set side by side align by it, so their boxes
 * stay level when one label wraps at a large font size and the other does not, or only one has a label.
 */
internal val FieldBoxTop = HorizontalAlignmentLine(::min)

private val BoxTopAtZero: Map<AlignmentLine, Int> = mapOf(FieldBoxTop to 0)

private fun Modifier.fieldBoxTop(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    layout(placeable.width, placeable.height, BoxTopAtZero) { placeable.place(0, 0) }
}

private fun Modifier.fieldBox(
    treatment: FieldTreatment,
    shape: Shape,
    fill: Color,
    edge: State<Color>,
    strokeWidth: Float,
): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = strokeWidth.dp.toPx()
    onDrawBehind {
        when (treatment) {
            FieldTreatment.Underlined -> {
                val y = size.height - stroke / 2f
                drawLine(edge.value, Offset(0f, y), Offset(size.width, y), stroke)
            }
            else -> {
                if (fill.alpha > 0f) drawOutline(outline, fill)
                drawOutline(outline, edge.value, style = Stroke(stroke))
            }
        }
    }
}
