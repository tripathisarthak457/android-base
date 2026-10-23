package com.base.app.core.designsystem.component.datadisplay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme

/** A dense two-column grid for records, where a list row would waste half the width. */
@Composable
fun AppKeyValueGrid(
    entries: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    striped: Boolean = true,
) {
    val colors = AppTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        entries.forEachIndexed { index, (key, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (striped && index % 2 == 1) colors.fill else Color.Transparent,
                    )
                    .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                AppText(
                    text = key,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.subheadline,
                    color = colors.contentTertiary,
                )
                AppText(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.typography.subheadline,
                    color = colors.contentPrimary,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}
