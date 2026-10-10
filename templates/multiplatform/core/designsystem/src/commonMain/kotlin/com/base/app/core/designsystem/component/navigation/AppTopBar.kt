package com.base.app.core.designsystem.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.interaction.PressFeedback
import com.base.app.core.designsystem.foundation.interaction.pressable
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.resources.Res
import com.base.app.core.designsystem.resources.designsystem_navigate_back
import com.base.app.core.designsystem.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The inline top bar of a pushed screen: a back button and the title centred in the bar. It pads
 * itself below the status bar; a bar that is not at the top of the window passes
 * `WindowInsets(0)` as [windowInsets].
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    windowInsets: WindowInsets = WindowInsets.statusBars,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .windowInsetsPadding(windowInsets)
            .height(AppTheme.sizes.topBarHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .pressable(
                        onClick = onBack,
                        feedback = PressFeedback.Dim,
                        onClickLabel = stringResource(Res.string.designsystem_navigate_back),
                    )
                    .size(AppTheme.sizes.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                AppIcon(
                    AppIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.designsystem_navigate_back),
                    tint = AppTheme.colors.accentContent,
                    size = AppTheme.sizes.iconLarge,
                )
            }
        }
        AppText(
            text = title,
            modifier = Modifier
                .padding(horizontal = AppTheme.sizes.minTouchTarget)
                .semantics { heading() },
            style = AppTheme.typography.headline,
            color = AppTheme.colors.contentPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * A root screen's large title, placed as the first item of its scrolling content so it scrolls
 * away with it, as iOS's does before the bar collapses.
 */
@Composable
fun AppLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.spacing.gutter)
            .padding(top = AppTheme.spacing.sm, bottom = AppTheme.spacing.md),
    ) {
        AppText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = AppTheme.typography.largeTitle,
            color = AppTheme.colors.contentPrimary,
        )
        if (subtitle != null) {
            AppText(
                text = subtitle,
                style = AppTheme.typography.subheadline,
                color = AppTheme.colors.contentSecondary,
            )
        }
    }
}
