package com.base.app.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.tokens.DesignTokens

/**
 * A text field over a [TextFieldState] the ViewModel owns, so typing never round-trips through UI
 * state and the caret never jumps. [error] replaces [helper] under the field and outlines it in
 * the danger colour; it is also what a screen reader announces as the field's error.
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
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onKeyboardAction: KeyboardActionHandler? = null,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = AppTheme.colors
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val outline = when {
        error != null -> colors.danger
        focused -> colors.accent
        else -> colors.fill
    }

    Column(modifier = modifier) {
        if (label != null) {
            AppText(
                text = label,
                modifier = Modifier.padding(bottom = AppTheme.spacing.xs),
                style = AppTheme.typography.footnote,
                color = colors.contentSecondary,
            )
        }
        BasicTextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (error != null) error(error) },
            enabled = enabled,
            textStyle = AppTheme.typography.body.copy(
                color = if (enabled) colors.contentPrimary else colors.contentDisabled,
            ),
            lineLimits = lineLimits,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = onKeyboardAction,
            interactionSource = source,
            cursorBrush = SolidColor(colors.accent),
            decorator = { innerTextField ->
                Box(
                    modifier = Modifier
                        .heightIn(min = AppTheme.sizes.fieldHeight)
                        .background(colors.fill, AppTheme.style.fieldShape)
                        .border(DesignTokens.Borders.width, outline, AppTheme.style.fieldShape)
                        .padding(horizontal = AppTheme.spacing.md),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (placeholder != null && state.text.isEmpty()) {
                        AppText(text = placeholder, style = AppTheme.typography.body, color = colors.contentTertiary)
                    }
                    innerTextField()
                }
            },
        )
        val supporting = error ?: helper
        if (supporting != null) {
            AppText(
                text = supporting,
                modifier = Modifier.padding(top = AppTheme.spacing.xs),
                style = AppTheme.typography.footnote,
                color = if (error != null) colors.danger else colors.contentSecondary,
            )
        }
    }
}
