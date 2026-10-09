package com.base.app.core.designsystem.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.AppLinkButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A search bar, as iOS draws one: a rounded well with a magnifier, a clear button once there is text,
 * and, while it has focus, a Cancel that clears it and lets go of the keyboard. [scopes] adds a
 * segmented control under it while searching ("All", "People", "Posts").
 */
@Composable
fun AppSearchField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.designsystem_search),
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    scopes: List<String> = emptyList(),
    selectedScope: Int = 0,
    onScopeChange: (Int) -> Unit = {},
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val transitions = rememberAppTransitions()
    val fade = tween<Float>(AppTheme.springs.fadeQuick)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            BasicTextField(
                state = state,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                textStyle = AppTheme.typography.body.copy(color = colors.contentPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                onKeyboardAction = { onSearch?.invoke() },
                lineLimits = TextFieldLineLimits.SingleLine,
                interactionSource = source,
                cursorBrush = SolidColor(colors.accent),
                decorator = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = SearchHeight)
                            .background(colors.fill, AppTheme.shapes.sm)
                            .padding(start = spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        AppIcon(AppIcons.Search, contentDescription = null, tint = colors.contentSecondary, size = AppTheme.sizes.iconSmall)
                        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (state.text.isEmpty()) {
                                AppText(text = placeholder, style = AppTheme.typography.body, color = colors.contentSecondary, maxLines = 1)
                            }
                            innerTextField()
                        }
                        AnimatedVisibility(visible = state.text.isNotEmpty(), enter = transitions.popIn, exit = transitions.popOut) {
                            AppIconButton(
                                icon = AppIcons.XCircle,
                                contentDescription = stringResource(R.string.designsystem_clear_search),
                                onClick = { state.clearText() },
                                size = ButtonSize.Small,
                            )
                        }
                    }
                },
            )
            if (onCancel != null) {
                AnimatedVisibility(
                    visible = focused,
                    enter = expandHorizontally(AppTheme.springs.layout.size) + fadeIn(fade),
                    exit = shrinkHorizontally(AppTheme.springs.layout.size) + fadeOut(fade),
                ) {
                    AppLinkButton(
                        text = stringResource(R.string.designsystem_cancel),
                        onClick = {
                            state.clearText()
                            focusManager.clearFocus()
                            onCancel()
                        },
                        modifier = Modifier.padding(start = 2.dp),
                    )
                }
            }
        }
        if (scopes.isNotEmpty()) {
            AnimatedVisibility(visible = focused || state.text.isNotEmpty(), enter = transitions.expandIn, exit = transitions.collapseOut) {
                AppSegmentedControl(options = scopes, selected = selectedScope, onSelect = onScopeChange)
            }
        }
    }
}

private val SearchHeight = 36.dp
