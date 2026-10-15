package com.base.app.core.designsystem.component.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
import kotlin.math.roundToInt

/**
 * A search bar, as iOS draws one: a rounded well with a magnifier, a clear button once there is text,
 * and, while it has focus, a Cancel that clears it and lets go of the keyboard. [scopes] adds a
 * segmented control under it while searching ("All", "People", "Posts").
 *
 * Cancel slides in from the trailing edge as the well shortens to make room. Both follow one
 * progress, read where they are placed and drawn: the field is measured once, at the width it has
 * beside Cancel, so focusing it re-measures nothing.
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
    val springs = AppTheme.springs
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val transitions = rememberAppTransitions()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(focused) { progress.animateTo(if (focused) 1f else 0f, springs.sheet.unit) }
    val wellShape = AppTheme.shapes.sm
    val cancelRoom = remember { CancelRoom() }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .drawWithCache {
                    val corner = CornerRadius((wellShape as? CornerBasedShape)?.topStart?.toPx(size, this) ?: 0f)
                    onDrawBehind {
                        val wellWidth = size.width - cancelRoom.width * progress.value
                        val start = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - wellWidth
                        drawRoundRect(colors.fill, Offset(start, 0f), Size(wellWidth, size.height), corner)
                    }
                },
            content = {
                BasicTextField(
                    state = state,
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
                            AnimatedVisibility(
                                visible = state.text.isNotEmpty(),
                                // The field keeps Cancel's room even unfocused; the clear button
                                // rides out to the well's end with it.
                                modifier = Modifier.offset { IntOffset(((1f - progress.value) * cancelRoom.width).roundToInt(), 0) },
                                enter = transitions.popIn,
                                exit = transitions.popOut,
                            ) {
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
                    AppLinkButton(
                        text = stringResource(R.string.designsystem_cancel),
                        onClick = {
                            state.clearText()
                            focusManager.clearFocus()
                            onCancel()
                        },
                        enabled = focused,
                        modifier = Modifier
                            .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }
                            .then(if (focused) Modifier else Modifier.clearAndSetSemantics {}),
                    )
                }
            },
        ) { measurables, constraints ->
            val gap = spacing.sm.roundToPx()
            val cancel = measurables.getOrNull(1)?.measure(Constraints())
            val room = cancel?.let { it.width + gap } ?: 0
            cancelRoom.width = room.toFloat()
            val field = measurables[0].measure(constraints.copy(minWidth = 0, maxWidth = (constraints.maxWidth - room).coerceAtLeast(0)))
            val height = maxOf(field.height, cancel?.height ?: 0)
            layout(constraints.maxWidth, height) {
                field.placeRelative(0, (height - field.height) / 2)
                if (cancel != null) {
                    // Rests just inside the trailing edge once focused, and just outside it otherwise.
                    val x = constraints.maxWidth - room * progress.value + gap
                    cancel.placeRelative(x.roundToInt(), (height - cancel.height) / 2)
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

/** How much of the width Cancel takes once shown, as last measured; read while drawing the well. */
private class CancelRoom {
    var width = 0f
}

private val SearchHeight = 36.dp
