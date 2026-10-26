package com.base.app.feature.sample.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.data.sample.api.model.SampleItem

/** One sample as a tappable card: the list's rows and the detail screen's related rows. */
@Composable
internal fun SampleRow(item: SampleItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    AppCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        AppText(
            text = item.title,
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        AppText(
            text = item.preview,
            modifier = Modifier.padding(top = AppTheme.spacing.xs),
            style = AppTheme.typography.subheadline,
            color = AppTheme.colors.contentTertiary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
