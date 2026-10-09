package com.base.app.catalog.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.ButtonSize
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.feedback.AppActivityIndicator
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.feedback.AppAvatarGroup
import com.base.app.core.designsystem.component.feedback.AppBadge
import com.base.app.core.designsystem.component.feedback.AppBadgedBox
import com.base.app.core.designsystem.component.feedback.AppBanner
import com.base.app.core.designsystem.component.feedback.AppDotBadge
import com.base.app.core.designsystem.component.feedback.AppLinearProgress
import com.base.app.core.designsystem.component.feedback.AppSkeleton
import com.base.app.core.designsystem.component.feedback.AppSkeletonCircle
import com.base.app.core.designsystem.component.feedback.AppSkeletonLine
import com.base.app.core.designsystem.component.feedback.AppSkeletonListItem
import com.base.app.core.designsystem.component.feedback.AppStateView
import com.base.app.core.designsystem.component.feedback.AppStatusPill
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.feedback.StateKind
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme

@Composable
fun FeedbackSection() {
    CatalogGroup("State view", caption = "Empty, error and offline, each with its action. It scrolls when the window is too short for it.") {
        Box(Modifier.fillMaxWidth().height(280.dp)) {
            AppStateView(title = "No messages yet", message = "Start a conversation and it will show here.", icon = AppIcons.Message, actionLabel = "New message", onAction = {})
        }
        Box(Modifier.fillMaxWidth().height(260.dp)) { AppStateView(kind = StateKind.Error, message = "The server did not answer.", onAction = {}) }
        Box(Modifier.fillMaxWidth().height(260.dp)) { AppStateView(kind = StateKind.Offline, message = "Showing what was saved last time.", onAction = {}) }
    }
    CatalogGroup("Banner") {
        AppBanner(text = "You are offline. Changes will sync later.", tone = AppTone.Warning)
        AppBanner(text = "A new version is available.", tone = AppTone.Info, actionLabel = "Update", onAction = {})
    }
    CatalogGroup("Skeletons", caption = "One shared sweep, which stops when no skeleton is on screen. With motion reduced they hold still.") {
        AppSkeletonListItem()
        AppSkeletonLine(widthFraction = 0.7f)
        AppSkeletonCircle()
        AppSkeleton(Modifier.fillMaxWidth().height(80.dp), shape = AppTheme.shapes.md)
    }
    CatalogGroup("Progress", caption = "iOS's spokes step rather than spin, so they redraw eight times a turn, not every frame.") {
        var progress by remember { mutableFloatStateOf(0.3f) }
        Wrap {
            AppActivityIndicator()
            AppActivityIndicator(size = 32.dp, color = AppTheme.colors.accent)
        }
        AppLinearProgress(progress = progress)
        AppButton(text = "Advance", onClick = { progress = if (progress >= 1f) 0f else progress + 0.2f }, variant = ButtonVariant.Tinted, size = ButtonSize.Small)
    }
    CatalogGroup("Badges and pills") {
        Wrap {
            AppBadge(count = 3)
            AppBadge(count = 120)
            AppBadgedBox(count = 5) { AppIcon(AppIcons.Bell, contentDescription = "Notifications", size = AppTheme.sizes.iconLarge) }
            AppDotBadge(visible = true) { AppIcon(AppIcons.Message, contentDescription = "Messages", size = AppTheme.sizes.iconLarge) }
            AppStatusPill(text = "Delivered", status = AppTheme.colors.success, icon = AppIcons.Check)
            AppStatusPill(text = "Pending", status = AppTheme.colors.warning)
            AppStatusPill(text = "Failed", status = AppTheme.colors.danger)
        }
    }
    CatalogGroup("Avatars", caption = "Initials on a colour the name always maps to; a group overlaps and counts the rest.") {
        Wrap {
            AppAvatar(name = "Ada Lovelace")
            AppAvatar(name = "Grace Hopper", size = 48.dp)
            AppAvatar(name = "Linus")
        }
        AppAvatarGroup(names = listOf("Ada Lovelace", "Grace Hopper", "Alan Turing", "Katherine Johnson", "Edsger Dijkstra", "Barbara Liskov"))
    }
}
