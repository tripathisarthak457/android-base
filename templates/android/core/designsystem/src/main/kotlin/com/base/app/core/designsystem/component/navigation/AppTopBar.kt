package com.base.app.core.designsystem.component.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import androidx.compose.ui.res.stringResource
import com.base.app.core.designsystem.R

/** The top bar. */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    navigationContentDescription: String = stringResource(R.string.designsystem_navigate_back),
    onNavigationClick: (() -> Unit)? = null,
    centerTitle: Boolean = false,
    showDivider: Boolean = true,
    background: Color = AppTheme.colors.surface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = AppTheme.colors
    val dividerColor by animateColorAsState(
        targetValue = if (showDivider) colors.divider else Color.Transparent,
        animationSpec = tween(AppTheme.motion.quick),
        label = "topBarDivider",
    )

    val hasNavigation = navigationIcon != null && onNavigationClick != null

    // Offset so the glyph, not its 44dp target, lines up with the gutter.
    val slot = AppTheme.sizes.buttonMedium
    val glyphInset = (slot - AppTheme.sizes.icon) / 2
    val edge = AppTheme.spacing.gutter - glyphInset

    // Centred means centred in the bar, not centred in the gap between the two slots — those are
    // the same thing only when both slots are occupied by the same number of icons.
    val titleStart = when {
        centerTitle -> edge + slot
        hasNavigation -> edge + slot + AppTheme.spacing.xs
        else -> edge + glyphInset
    }
    val titleEnd = edge + slot

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTheme.sizes.topBarHeight),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = titleStart, end = titleEnd),
                horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                AppText(
                    text = title,
                    style = AppTheme.typography.headingMedium,
                    color = colors.contentPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                )
                subtitle?.let {
                    AppText(
                        text = it,
                        style = AppTheme.typography.caption,
                        color = colors.contentTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = edge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasNavigation) {
                    AppIconButton(
                        icon = navigationIcon,
                        contentDescription = navigationContentDescription,
                        onClick = onNavigationClick,
                        size = ButtonSize.Medium,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }

        AppDivider(color = dividerColor)
    }
}

/** The back-arrow bar, which is most of them. */
@Composable
fun AppBackTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showDivider: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    AppTopBar(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        navigationIcon = AppIcons.ArrowLeft,
        onNavigationClick = onBack,
        showDivider = showDivider,
        actions = actions,
    )
}

/** A large title that sits above the content rather than in a bar. For a root screen. */
@Composable
fun AppLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            // Smaller end inset so the action glyph, centred in its target, sits on the gutter.
            .padding(
                start = AppTheme.spacing.gutter,
                end = AppTheme.spacing.gutter - (AppTheme.sizes.buttonMedium - AppTheme.sizes.icon) / 2,
                top = AppTheme.spacing.lg,
                bottom = AppTheme.spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(
                text = title,
                style = AppTheme.typography.displaySmall,
                color = AppTheme.colors.contentPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.let {
                AppText(
                    text = it,
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colors.contentTertiary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}
