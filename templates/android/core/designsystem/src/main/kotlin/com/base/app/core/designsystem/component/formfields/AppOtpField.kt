package com.base.app.core.designsystem.component.formfields

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.layout.LocalTabVisible
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A one-time code: [length] boxes over one real field, so paste, the keyboard's suggestion and SMS
 * autofill all land at once. A screen reader hears the field by its [label], not six unnamed boxes.
 * [onFilled] runs when the last digit arrives.
 */
@Suppress("ktlint:compose:parameter-naming") // onFilled runs once, when it is full.
@Composable
public fun AppOtpField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    length: Int = 6,
    label: String? = null,
    supporting: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    autoFocus: Boolean = true,
    onFilled: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val focusRequester = remember { FocusRequester() }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val filled by rememberUpdatedState(onFilled)
    val input = remember(length) { DigitsOnly.then(InputTransformation.maxLength(length)) }

    LaunchedEffect(autoFocus) { if (autoFocus) focusRequester.requestFocus() }
    LaunchedEffect(state, length) {
        snapshotFlow { state.text.toString() }.collect { if (it.length == length) filled(it) }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        label?.let { AppText(text = it, style = AppTheme.typography.footnote, color = colors.contentSecondary) }
        BasicTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .semantics {
                    contentType = ContentType.SmsOtpCode
                    label?.let { contentDescription = it }
                    if (isError) error(supporting.orEmpty())
                },
            enabled = enabled,
            inputTransformation = input,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            interactionSource = source,
            cursorBrush = SolidColor(Color.Transparent),
            decorator = { innerTextField ->
                Box {
                    // The real field stays in the layout for focus and the IME, but draws nothing.
                    Box(Modifier.size(1.dp).graphicsLayer { alpha = 0f }) { innerTextField() }
                    Row(
                        modifier = Modifier.fillMaxWidth().clearAndSetSemantics {},
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    ) {
                        val text = state.text
                        repeat(length) { index ->
                            val digit = text.getOrNull(index)
                            val next = focused && index == text.length && text.length < length
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = AppOtpFieldDefaults.BoxHeight)
                                    .background(if (enabled) colors.surface else colors.fill, AppTheme.shapes.sm)
                                    .border(
                                        width = if (next || isError) AppTheme.style.borderWidthStrong else AppTheme.style.borderWidth,
                                        color = when {
                                            isError -> colors.danger.content
                                            next -> colors.accent
                                            else -> colors.outline
                                        },
                                        shape = AppTheme.shapes.sm,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    digit != null -> AppText(text = digit.toString(), style = AppTheme.typography.title2, color = colors.contentPrimary, textAlign = TextAlign.Center)
                                    next -> Caret()
                                }
                            }
                        }
                    }
                }
            },
        )
        supporting?.let {
            AppText(text = it, style = AppTheme.typography.footnote, color = if (isError) colors.danger.content else colors.contentSecondary)
        }
    }
}

/**
 * The blinking caret on the next box. It exists only on that box while the field has focus, blinks
 * in the draw phase, and holds still when the screen is hidden or motion is reduced.
 */
@Composable
private fun Caret() {
    val blinking = LocalTabVisible.current.isVisible && !AppTheme.reduceMotion
    val alpha = if (blinking) {
        rememberInfiniteTransition(label = "otpCaret").animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = AppTheme.springs.caretBlink(),
            label = "otpCaretAlpha",
        )
    } else {
        null
    }
    Box(
        Modifier
            .width(AppOtpFieldDefaults.CaretWidth)
            .height(AppOtpFieldDefaults.CaretHeight)
            .graphicsLayer { this.alpha = alpha?.value ?: 1f }
            .background(AppTheme.colors.accent),
    )
}

/** The code field's boxes and caret. */
public object AppOtpFieldDefaults {
    public val CaretWidth: Dp = 2.dp
    public val CaretHeight: Dp = 24.dp
    public val BoxHeight: Dp = 48.dp
}


@AppComponentPreviews
@Composable
private fun AppOtpFieldPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppOtpField(state = rememberTextFieldState(), label = "Code", supporting = "Sent to +44 7700 900123", autoFocus = false)
        AppOtpField(state = rememberTextFieldState("4821"), autoFocus = false)
        AppOtpField(state = rememberTextFieldState("482199"), isError = true, supporting = "That code has expired", autoFocus = false)
        AppOtpField(state = rememberTextFieldState("12"), length = 4, enabled = false, autoFocus = false)
    }
}
