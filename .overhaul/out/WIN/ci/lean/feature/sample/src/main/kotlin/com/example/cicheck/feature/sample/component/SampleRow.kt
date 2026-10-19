package com.example.cicheck.feature.sample.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.cicheck.core.designsystem.component.container.AppCard
import com.example.cicheck.core.designsystem.component.text.AppText
import com.example.cicheck.core.designsystem.theme.AppTheme
import com.example.cicheck.data.sample.api.SampleItem

/** One sample as a tappable card: the list's rows and the detail screen's related rows. */
@Composable
internal fun SampleRow(
    item: SampleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.titleLarge,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = item.preview,
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
            style = AppTheme.typography.bodySmall,
            color = AppTheme.colors.contentTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
