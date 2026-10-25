package com.base.app.core.designsystem.component.feedback

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
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.animation.rememberAppTransitions
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.draw.ContentUnderBone
import com.base.app.core.designsystem.foundation.draw.LocalRedacted
import com.base.app.core.designsystem.foundation.draw.skeleton
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.preview.AppComponentPreviews
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.StatusColors

/** A count in a red pill: "3", "99+". */
@Composable
fun AppBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxCount: Int = 99,
) {
    val danger = AppTheme.colors.danger
    val bone = LocalRedacted.current
    val redacted = if (bone) Modifier.skeleton(AppTheme.shapes.pill) else Modifier
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
            .then(redacted)
            .background(danger.base, AppTheme.shapes.pill)
            .padding(horizontal = 5.dp),
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
fun AppBadgedBox(
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
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-6).dp),
        ) {
            AppBadge(count = count, maxCount = maxCount)
        }
    }
}

/** A bare dot, for "there is something new here" with no number to show. */
@Composable
fun AppDotBadge(
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
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp),
        ) {
            Box(Modifier.size(8.dp).background(AppTheme.colors.danger.base, AppTheme.shapes.pill).clearAndSetSemantics {})
        }
    }
}

/** A status pill: "Delivered", "Pending", "Failed". */
@Composable
fun AppStatusPill(
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
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentUnderBone(bone) {
            icon?.let { AppIcon(it, contentDescription = null, tint = status.content, size = 14.dp) }
            AppText(text = text, style = AppTheme.typography.caption1, color = status.content, maxLines = 1)
        }
    }
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
