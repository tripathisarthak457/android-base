package com.base.app.core.designsystem.component.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.resources.Res
import com.base.app.core.designsystem.resources.designsystem_clear_search
import com.base.app.core.designsystem.resources.designsystem_search
import com.base.app.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A search field as iOS draws one: a filled capsule with a magnifying glass, and a clear button
 * once there is something to clear. The text lives in [state], which the ViewModel owns.
 */
@Composable
fun AppSearchField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(Res.string.designsystem_search),
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val clearLabel = stringResource(Res.string.designsystem_clear_search)
    BasicTextField(
        state = state,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = AppTheme.typography.body.copy(color = colors.contentPrimary),
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = onSearch?.let { search -> KeyboardActionHandler { search() } },
        cursorBrush = SolidColor(colors.accent),
        decorator = { innerTextField ->
            Row(
                modifier = Modifier
                    .heightIn(min = AppTheme.sizes.buttonSmall)
                    .background(colors.fill, AppTheme.shapes.sm)
                    .padding(start = AppTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(
                    AppIcons.Search,
                    contentDescription = null,
                    tint = colors.contentTertiary,
                    size = AppTheme.sizes.iconSmall,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = AppTheme.spacing.xs),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (state.text.isEmpty()) {
                        AppText(text = placeholder, style = AppTheme.typography.body, color = colors.contentTertiary)
                    }
                    innerTextField()
                }
                if (state.text.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .pressable(
                                onClick = { state.clearText() },
                                feedback = PressFeedback.Dim,
                                onClickLabel = clearLabel,
                            )
                            .padding(AppTheme.spacing.sm),
                    ) {
                        AppIcon(
                            AppIcons.XCircle,
                            contentDescription = clearLabel,
                            tint = colors.contentTertiary,
                            size = AppTheme.sizes.iconSmall,
                        )
                    }
                }
            }
        },
    )
}
