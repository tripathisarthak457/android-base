package com.base.app.core.designsystem.component.fields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.input.fieldDecorator
import com.base.app.core.designsystem.component.overlay.AppMenu
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.PressFeedback
import com.base.app.core.designsystem.foundation.disabledAlpha
import com.base.app.core.designsystem.foundation.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A field that opens a menu of choices instead of a keyboard. The whole field is the target, not
 * just its chevron, and the current choice is marked in the menu.
 */
@Composable
fun <T> AppSelectField(
    value: T?,
    options: List<T>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = stringResource(R.string.designsystem_select),
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    optionLabel: (T) -> String = { it.toString() },
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = value?.let(optionLabel)
    val decorator = fieldDecorator(
        empty = { shown == null },
        focused = expanded,
        label = label,
        placeholder = placeholder,
        supporting = error ?: helper,
        isError = error != null,
        trailing = { AppIcon(AppIcons.ChevronDown, contentDescription = null, tint = AppTheme.colors.contentSecondary, size = 16.dp) },
    )
    Box(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .pressable(onClick = { expanded = true }, feedback = PressFeedback.None, enabled = enabled, role = Role.DropdownList)
                .disabledAlpha(enabled),
        ) {
            decorator.Decoration {
                if (shown != null) AppText(text = shown, style = AppTheme.typography.body, color = AppTheme.colors.contentPrimary, maxLines = 1)
            }
        }
        AppMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                AppMenuItem(
                    text = optionLabel(option),
                    icon = if (option == value) AppIcons.Check else null,
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * A phone number: a dialling code chosen from [dialCodes] beside the number itself, typed into a
 * [TextFieldState] that accepts digits only.
 */
@Composable
fun AppPhoneField(
    dialCode: String,
    onDialCodeChange: (String) -> Unit,
    number: TextFieldState,
    modifier: Modifier = Modifier,
    dialCodes: List<String> = DefaultDialCodes,
    label: String? = stringResource(R.string.designsystem_phone_number),
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        AppSelectField(
            value = dialCode,
            options = dialCodes,
            onSelect = onDialCodeChange,
            modifier = Modifier.width(DialCodeWidth),
            label = label,
            enabled = enabled,
        )
        AppTextField(
            state = number,
            modifier = Modifier.weight(1f),
            helper = helper,
            error = error,
            enabled = enabled,
            maxLength = MAX_NUMBER_LENGTH,
            inputTransformation = DigitsOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        )
    }
}

/** Refuses anything that is not a digit, typed or pasted. */
val DigitsOnly = InputTransformation {
    if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
}

/** A short default list. Replace it with the codes your product serves. */
val DefaultDialCodes: List<String> = listOf("+1", "+44", "+61", "+65", "+91", "+971")

private val DialCodeWidth = 108.dp
private const val MAX_NUMBER_LENGTH = 15
