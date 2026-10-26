package com.example.cmp.core.designsystem.component.formfields

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.R
import com.example.cmp.core.designsystem.component.input.AppTextField
import com.example.cmp.core.designsystem.component.selection.AppChip
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme

/**
 * Tags typed one at a time into [input] and kept as removable chips above it. Return adds the tag;
 * a blank or repeated tag is ignored, and the field says so once [maxTags] is reached.
 */
@Composable
public fun AppTagField(
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
            // A tag's remove target is 48 dp, past the chip's edges: rows this far apart keep each
            // target whole and clear of the next row's.
            FlowRow(
                modifier = Modifier.padding(vertical = AppTagFieldDefaults.TargetOverhang),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
                verticalArrangement = Arrangement.spacedBy(AppTagFieldDefaults.TargetOverhang * 2),
            ) {
                tags.forEach { tag ->
                    AppChip(label = tag, onClick = null, enabled = enabled, onRemove = { onTagsChange(tags - tag) })
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

/** The tag field's metrics. */
public object AppTagFieldDefaults {
    /** How far a tag's 48 dp remove target reaches past its 32 dp chip, above and below. */
    public val TargetOverhang: Dp = 8.dp
}

@AppComponentPreviews
@Composable
private fun AppTagFieldPreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        AppTagField(tags = emptyList(), onTagsChange = {}, input = rememberTextFieldState(), label = "Tags")
        AppTagField(
            tags = listOf("hiking", "weekend", "a much longer tag than the others"),
            onTagsChange = {},
            input = rememberTextFieldState("ca"),
            label = "Tags",
        )
        AppTagField(tags = listOf("archived"), onTagsChange = {}, input = rememberTextFieldState(), enabled = false)
    }
}
