package com.base.app.core.designsystem.component.formfields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.input.FieldBoxTop
import com.base.app.core.designsystem.component.input.fieldDecorator
import com.base.app.core.designsystem.component.overlay.AppMenu
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.component.text.scaledLike
import com.base.app.core.designsystem.foundation.draw.disabledAlpha
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme

/**
 * A field that opens a menu of choices instead of a keyboard. The whole field is the target, not
 * just its chevron, and the current choice is marked in the menu.
 */
@Composable
public fun <T> AppSelectField(
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
        trailing = {
            AppIcon(
                AppIcons.ChevronDown,
                contentDescription = null,
                tint = AppTheme.colors.contentSecondary,
                size = AppTheme.sizes.iconSmall.scaledLike(AppTheme.typography.body),
            )
        },
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
 * [TextFieldState] that accepts digits only. Each part carries its own label, so a screen reader
 * names both.
 */
@Composable
public fun AppPhoneField(
    dialCode: String,
    onDialCodeChange: (String) -> Unit,
    number: TextFieldState,
    modifier: Modifier = Modifier,
    dialCodes: List<String> = DefaultDialCodes,
    label: String? = stringResource(R.string.designsystem_phone_number),
    dialCodeLabel: String? = stringResource(R.string.designsystem_dialling_code),
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        AppSelectField(
            value = dialCode,
            options = dialCodes,
            onSelect = onDialCodeChange,
            // The code and its chevron grow with the text, so a fixed box would cut the longest code.
            modifier = Modifier.alignBy(FieldBoxTop).width(AppPhoneFieldDefaults.DialCodeWidth.scaledLike(AppTheme.typography.body)),
            label = dialCodeLabel,
            enabled = enabled,
        )
        AppTextField(
            state = number,
            modifier = Modifier.alignBy(FieldBoxTop).weight(1f),
            label = label,
            helper = helper,
            error = error,
            enabled = enabled,
            maxLength = MaxNumberLength,
            inputTransformation = DigitsOnly,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
        )
    }
}

/** Refuses anything that is not a digit, typed or pasted. */
public val DigitsOnly: InputTransformation = InputTransformation {
    if (!asCharSequence().all(Char::isDigit)) revertAllChanges()
}

/** A short default list. Replace it with the codes your product serves. */
public val DefaultDialCodes: List<String> = listOf("+1", "+44", "+61", "+65", "+91", "+971")

/** The phone field's dialling-code column. */
public object AppPhoneFieldDefaults {
    public val DialCodeWidth: Dp = 108.dp
}

private const val MaxNumberLength = 15

@AppComponentPreviews
@Composable
private fun AppSelectFieldPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppSelectField(value = null, options = listOf("Small", "Medium", "Large"), onSelect = {}, label = "Size")
        AppSelectField(value = "Medium", options = listOf("Small", "Medium", "Large"), onSelect = {}, label = "Size", helper = "Fits true to size")
        AppSelectField(value = null, options = listOf("UK", "France"), onSelect = {}, label = "Country", error = "Choose where to ship")
        AppSelectField(value = "Large", options = listOf("Large"), onSelect = {}, label = "Size", enabled = false)
        AppPhoneField(dialCode = "+44", onDialCodeChange = {}, number = rememberTextFieldState("7700 900123"))
        AppPhoneField(dialCode = "+1", onDialCodeChange = {}, number = rememberTextFieldState("555"), error = "Enter the full number")
    }
}
