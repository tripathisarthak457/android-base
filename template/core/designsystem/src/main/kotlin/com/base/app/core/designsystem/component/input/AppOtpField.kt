package com.base.app.core.designsystem.component.input

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.clickableNoIndication
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A one-time-code field: [length] boxes fed by a single hidden text field.
 *
 * ## One field, not N
 *
 * The obvious build — one `BasicTextField` per box, each advancing focus to the next — is the one
 * everybody regrets. Backspace on an empty box has to move focus back and delete the previous
 * character; pasting a six-digit code has to be split across six fields; and the SMS autofill
 * suggestion only ever populates the field it was attached to. One invisible field holding the
 * whole value makes paste, autofill and backspace work for free, and the boxes become pure
 * decoration drawn from its text.
 *
 * ## Autofill
 *
 * `KeyboardOptions(autoCorrectEnabled = false)` plus the numeric keyboard is what lets the
 * platform offer an incoming SMS code above the keyboard on most devices.
 *
 * [onFilled] fires the moment the last digit lands, so the caller submits without the user having
 * to reach for a button they can no longer see behind the keyboard.
 */
@Composable
fun AppOtpField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
    label: String? = null,
    supporting: String? = null,
    autoFocus: Boolean = true,
    boxSize: Dp = 48.dp,
    onFilled: (String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val focusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    // The cursor is drawn by us, on the box that is next to be filled — there is no real caret to
    // show, because the text field itself has zero size.
    val caret = rememberInfiniteTransition(label = "otpCaret")
    val caretAlpha by caret.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(CARET_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "otpCaretAlpha",
    )

    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        label?.let {
            AppText(
                text = it,
                style = AppTheme.typography.titleSmall,
                color = colors.contentSecondary,
            )
        }

        Box {
            BasicTextField(
                // The selection is pinned to the end so a tap anywhere in the (invisible) field
                // cannot land the caret in the middle of the code, where the next digit would be
                // inserted where nobody expects it.
                value = TextFieldValue(text = value, selection = androidx.compose.ui.text.TextRange(value.length)),
                onValueChange = { candidate ->
                    val digits = candidate.text.filter(Char::isDigit).take(length)
                    if (digits != value) {
                        onValueChange(digits)
                        if (digits.length == length) onFilled(digits)
                    }
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
                    .focusRequester(focusRequester),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions.Default,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color.Transparent),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            ) {
                repeat(length) { index ->
                    val digit = value.getOrNull(index)
                    val isNext = focused && index == value.length.coerceAtMost(length - 1) && value.length < length

                    val border by animateColorAsState(
                        targetValue = when {
                            isError -> colors.danger.content
                            isNext -> colors.accent
                            digit != null -> colors.borderStrong
                            else -> colors.border
                        },
                        animationSpec = tween(AppTheme.motion.quick),
                        label = "otpBoxBorder",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(boxSize)
                            .clip(AppTheme.shapes.sm)
                            .background(if (enabled) colors.surface else colors.surfaceVariant)
                            .border(
                                width = if (isNext || isError) {
                                    AppTheme.sizes.borderWidthStrong
                                } else {
                                    AppTheme.sizes.borderWidth
                                },
                                color = border,
                                shape = AppTheme.shapes.sm,
                            )
                            // Tapping any box focuses the one real field behind them.
                            .clickableNoIndication(enabled = enabled) { focusRequester.requestFocus() },
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            digit != null -> AppText(
                                text = digit.toString(),
                                style = AppTheme.typography.headingLarge,
                                color = colors.contentPrimary,
                                textAlign = TextAlign.Center,
                            )

                            isNext -> Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(boxSize / 2)
                                    .alpha(caretAlpha)
                                    .background(colors.accent),
                            )
                        }
                    }
                }
            }
        }

        supporting?.let {
            AppText(
                text = it,
                style = AppTheme.typography.caption,
                color = if (isError) colors.danger.content else colors.contentTertiary,
            )
        }
    }
}

private const val CARET_MILLIS = 550
