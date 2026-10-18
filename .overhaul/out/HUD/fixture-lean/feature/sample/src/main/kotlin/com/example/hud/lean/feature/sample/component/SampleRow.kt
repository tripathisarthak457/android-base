package com.example.hud.lean.feature.sample.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.hud.lean.core.designsystem.component.container.AppCard
import com.example.hud.lean.core.designsystem.component.text.AppText
import com.example.hud.lean.core.designsystem.theme.AppTheme
import com.example.hud.lean.data.sample.api.model.SampleItem

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
