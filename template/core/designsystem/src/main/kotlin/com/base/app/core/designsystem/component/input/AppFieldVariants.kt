package com.base.app.core.designsystem.component.input

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.icon.AppIcons
import androidx.compose.ui.res.stringResource
import com.base.app.core.designsystem.R

/** A password field with a reveal toggle. Visibility is local state rather than hoisted. */
@Composable
fun AppPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var revealed by remember { mutableStateOf(false) }

    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helper = helper,
        error = error,
        enabled = enabled,
        singleLine = true,
        leadingIcon = AppIcons.Lock,
        trailing = {
            AppIconButton(
                icon = if (revealed) AppIcons.EyeOff else AppIcons.Eye,
                contentDescription = stringResource(
                    if (revealed) R.string.designsystem_hide_password else R.string.designsystem_show_password,
                ),
                onClick = { revealed = !revealed },
                size = ButtonSize.Small,
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
    )
}

/** A search field. The clear button appears only when there is something to clear. */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = stringResource(R.string.designsystem_search),
    enabled: Boolean = true,
    onSearch: (() -> Unit)? = null,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = true,
        leadingIcon = AppIcons.Search,
        trailing = if (value.isNotEmpty()) {
            {
                AppIconButton(
                    icon = AppIcons.Close,
                    contentDescription = stringResource(R.string.designsystem_clear_search),
                    onClick = { onValueChange("") },
                    size = ButtonSize.Small,
                )
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
    )
}

/**
 * A multi-line field for free text. [minLines] rather than a fixed height, so the box starts at a
 * size that invites a paragraph and still grows with the content instead of scrolling inside four
 * lines.
 */
@Composable
fun AppTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    minLines: Int = 4,
    maxLines: Int = 8,
    maxLength: Int? = null,
) {
    AppTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helper = helper,
        error = error,
        enabled = enabled,
        singleLine = false,
        minLines = minLines,
        maxLines = maxLines,
        maxLength = maxLength,
        showCounter = maxLength != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default,
        ),
    )
}

/** A numeric field that only ever receives digits. */
@Composable
fun AppNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helper: String? = null,
    error: String? = null,
    enabled: Boolean = true,
    maxLength: Int? = null,
    imeAction: ImeAction = ImeAction.Done,
) {
    AppTextField(
        value = value,
        onValueChange = { candidate -> onValueChange(candidate.filter(Char::isDigit)) },
        modifier = modifier,
        label = label,
        placeholder = placeholder,
        helper = helper,
        error = error,
        enabled = enabled,
        singleLine = true,
        maxLength = maxLength,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
    )
}
