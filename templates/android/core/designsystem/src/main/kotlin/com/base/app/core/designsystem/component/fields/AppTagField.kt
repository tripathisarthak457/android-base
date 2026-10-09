package com.base.app.core.designsystem.component.fields

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.base.app.core.designsystem.R
import com.base.app.core.designsystem.component.input.AppTextField
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.theme.AppTheme

/**
 * Tags typed one at a time into [input] and kept as removable chips above it. Return adds the tag;
 * a blank or repeated tag is ignored, and the field says so once [maxTags] is reached.
 */
@Composable
fun AppTagField(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    input: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = stringResource(R.string.designsystem_add_a_tag),
    maxTags: Int = 10,
    enabled: Boolean = true,
) {
    val full = tags.size >= maxTags
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
        if (tags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs)) {
                tags.forEach { tag ->
                    AppChip(label = tag, onClick = {}, enabled = enabled, onRemove = { onTagsChange(tags - tag) })
                }
            }
        }
        AppTextField(
            state = input,
            label = label,
            placeholder = placeholder,
            helper = if (full) stringResource(R.string.designsystem_tag_limit_reached) else null,
            enabled = enabled && !full,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = {
                val tag = input.text.toString().trim()
                if (tag.isNotEmpty() && tag !in tags && !full) onTagsChange(tags + tag)
                input.clearText()
            },
        )
    }
}
