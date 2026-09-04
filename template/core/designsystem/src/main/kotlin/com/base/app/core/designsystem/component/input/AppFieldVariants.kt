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

/**
 * A password field with a reveal toggle.
 *
 * Visibility is local state rather than hoisted. It is a momentary display preference that should
 * reset every time the screen is recreated — a ViewModel remembering that the password was
 * revealed, and restoring that after the app returns from the background, is a small privacy leak
 * nobody asked for.
 */
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
                contentDescription = if (revealed) "Hide password" else "Show password",
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

/**
 * A search field.
 *
 * The clear button appears only when there is something to clear. A permanently visible clear
 * affordance on an empty field is a control that does nothing, and it competes with the
 * placeholder for the same space.
 */
@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
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
                    contentDescription = "Clear search",
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
 * A multi-line field for free text.
 *
 * [minLines] rather than a fixed height, so the box starts at a size that invites a paragraph and
 * still grows with the content instead of scrolling inside four lines.
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

/**
 * A numeric field that only ever receives digits.
 *
 * Filtering here rather than trusting the keyboard type matters: `KeyboardType.Number` is a hint,
 * not a constraint. A physical keyboard, a paste, a voice input or several third-party IMEs will
 * all happily deliver letters into a field marked numeric.
 */
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
