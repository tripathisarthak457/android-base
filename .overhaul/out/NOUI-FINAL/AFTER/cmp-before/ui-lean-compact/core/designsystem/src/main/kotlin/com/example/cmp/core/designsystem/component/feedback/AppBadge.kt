package com.example.cmp.core.designsystem.component.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cmp.core.designsystem.animation.rememberAppTransitions
import com.example.cmp.core.designsystem.component.text.AppIcon
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.draw.ContentUnderBone
import com.example.cmp.core.designsystem.foundation.draw.LocalRedacted
import com.example.cmp.core.designsystem.foundation.draw.skeleton
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.designsystem.preview.AppComponentPreviews
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.designsystem.theme.StatusColors

/** A count in a red pill: "3", "99+". */
@Composable
public fun AppBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
) {
    val danger = AppTheme.colors.danger
    val bone = LocalRedacted.current
    val redacted = if (bone) Modifier.skeleton(AppTheme.shapes.pill) else Modifier
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = AppBadgeDefaults.MinSize, minHeight = AppBadgeDefaults.MinSize)
            .then(redacted)
            .background(danger.base, AppTheme.shapes.pill)
            .padding(horizontal = AppBadgeDefaults.HorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        ContentUnderBone(bone) {
            AppText(
                text = if (count > maxCount) "$maxCount+" else count.toString(),
                style = AppTheme.typography.caption2,
                color = danger.on,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** [content] with an [AppBadge] on its top trailing corner while [count] is above zero. */
@Composable
public fun AppBadgedBox(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
    content: @Composable () -> Unit,
) {
    val transitions = rememberAppTransitions()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
        AnimatedVisibility(
            visible = count > 0,
            enter = transitions.popIn,
            exit = transitions.popOut,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = AppBadgeDefaults.CountOffsetX, y = AppBadgeDefaults.CountOffsetY),
        ) {
            AppBadge(count = count, maxCount = maxCount)
        }
    }
}

/** A bare dot, for "there is something new here" with no number to show. */
@Composable
public fun AppDotBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transitions = rememberAppTransitions()
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        content()
        AnimatedVisibility(
            visible = visible,
            enter = transitions.popIn,
            exit = transitions.popOut,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = AppBadgeDefaults.DotOffsetX, y = AppBadgeDefaults.DotOffsetY),
        ) {
            Box(Modifier.size(AppBadgeDefaults.DotSize).background(AppTheme.colors.danger.base, AppTheme.shapes.pill).clearAndSetSemantics {})
        }
    }
}

/** A status pill: "Delivered", "Pending", "Failed". */
@Composable
public fun AppStatusPill(
    text: String,
    status: StatusColors,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val bone = LocalRedacted.current
    val redacted = if (bone) Modifier.skeleton(AppTheme.shapes.pill) else Modifier
    Row(
        modifier = modifier
            .then(redacted)
            .background(status.subtle, AppTheme.shapes.pill)
            .padding(horizontal = AppBadgeDefaults.PillHorizontalPadding, vertical = AppBadgeDefaults.PillVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(AppBadgeDefaults.PillIconSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentUnderBone(bone) {
            icon?.let { AppIcon(it, contentDescription = null, tint = status.content, size = AppBadgeDefaults.PillIconSize) }
            AppText(text = text, style = AppTheme.typography.caption1, color = status.content, maxLines = 1)
        }
    }
}

/** The badges' and the status pill's metrics. */
public object AppBadgeDefaults {
    public val MinSize: Dp = 18.dp
    public val HorizontalPadding: Dp = 5.dp
    public val CountOffsetX: Dp = 8.dp
    public val CountOffsetY: Dp = (-6).dp
    public val DotSize: Dp = 8.dp
    public val DotOffsetX: Dp = 2.dp
    public val DotOffsetY: Dp = (-2).dp
    public val PillHorizontalPadding: Dp = 10.dp
    public val PillVerticalPadding: Dp = 4.dp
    public val PillIconSpacing: Dp = 4.dp
    public val PillIconSize: Dp = 14.dp
}

@AppComponentPreviews
@Composable
private fun AppBadgePreview() {
    Column(
        modifier = Modifier.background(AppTheme.colors.background).padding(AppTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg)) {
            AppBadge(count = 3)
            AppBadge(count = 120)
            AppBadgedBox(count = 8) { AppIcon(AppIcons.Bell, contentDescription = "Notifications") }
            AppDotBadge(visible = true) { AppIcon(AppIcons.Message, contentDescription = "Messages") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm)) {
            AppStatusPill(text = "Paid", status = AppTheme.colors.success, icon = AppIcons.Check)
            AppStatusPill(text = "Overdue", status = AppTheme.colors.danger)
            AppStatusPill(text = "Pending review", status = AppTheme.colors.warning)
        }
    }
}
