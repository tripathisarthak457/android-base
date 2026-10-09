package com.base.app.core.designsystem.component.container

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.foundation.HairlineEdge
import com.base.app.core.designsystem.foundation.hairline
import com.base.app.core.designsystem.theme.AppTheme

/**
 * The sticky action at the bottom of a screen that has no tab bar (a full-screen destination, a
 * sheet): a checkout's Pay, a composer's Send. It rides above the keyboard and the navigation bar.
 * Pass it as `AppScaffold(bottomBar = …)`, which keeps the content's last item above it. On a tab
 * root the bar's accessory slot does this job instead.
 */
@Composable
fun AppBottomActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .hairline(HairlineEdge.Top)
            .background(AppTheme.colors.surface)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = AppTheme.spacing.gutter, vertical = AppTheme.spacing.md),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
