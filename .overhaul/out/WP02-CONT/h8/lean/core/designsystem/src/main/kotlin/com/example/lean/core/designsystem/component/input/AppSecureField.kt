package com.example.lean.core.designsystem.component.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.lean.core.designsystem.R
import com.example.lean.core.designsystem.component.button.FieldIconButton
import com.example.lean.core.designsystem.foundation.draw.disabledAlpha
import com.example.lean.core.designsystem.icon.AppIcons
import com.example.lean.core.designsystem.theme.AppTheme

/**
 * A password field. The text is held by the platform's secure field, which keeps it out of the
 * clipboard and shows the last character typed for a moment, and a reveal button toggles it. Whether
 * it is revealed is local to the screen and survives rotation.
 */
@Composable
fun AppSecureField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    onKeyboardAction: KeyboardActionHandler? = null,
) {
    var revealed by rememberSaveable { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val colors = AppTheme.colors

    BasicSecureTextField(
        state = state,
        modifier = modifier
            .fillMaxWidth()
            .disabledAlpha(enabled)
            .semantics { if (error != null) error(error) },
        enabled = enabled,
        textStyle = AppTheme.typography.body.copy(color = colors.contentPrimary),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        onKeyboardAction = onKeyboardAction,
        textObfuscationMode = if (revealed) TextObfuscationMode.Visible else TextObfuscationMode.RevealLastTyped,
        interactionSource = source,
        cursorBrush = SolidColor(if (error != null) colors.danger.content else colors.accent),
        decorator = fieldDecorator(
            empty = { state.text.isEmpty() },
            focused = focused,
            label = label,
            placeholder = placeholder,
            supporting = error ?: helper,
            isError = error != null,
            leadingIcon = AppIcons.Lock,
            trailing = {
                FieldIconButton(
                    icon = if (revealed) AppIcons.EyeOff else AppIcons.Eye,
                    contentDescription = stringResource(
                        if (revealed) R.string.designsystem_hide_password else R.string.designsystem_show_password,
                    ),
                    onClick = { revealed = !revealed },
                    textStyle = AppTheme.typography.body,
                )
            },
        ),
    )
}
