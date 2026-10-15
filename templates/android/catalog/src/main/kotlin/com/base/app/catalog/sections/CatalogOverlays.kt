package com.base.app.catalog.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.button.AppButton
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.button.ButtonVariant
import com.base.app.core.designsystem.component.container.AppBottomActionBar
import com.base.app.core.designsystem.component.feedback.AppLoadingOverlay
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.list.AppSection
import com.base.app.core.designsystem.component.overlay.AppActionSheet
import com.base.app.core.designsystem.component.overlay.AppAlert
import com.base.app.core.designsystem.component.overlay.AppContextMenu
import com.base.app.core.designsystem.component.overlay.AppMenu
import com.base.app.core.designsystem.component.overlay.AppMenuDivider
import com.base.app.core.designsystem.component.overlay.AppMenuItem
import com.base.app.core.designsystem.component.overlay.AppSheet
import com.base.app.core.designsystem.component.overlay.AppToast
import com.base.app.core.designsystem.component.overlay.LocalOverlayHost
import com.base.app.core.designsystem.component.overlay.SheetAction
import com.base.app.core.designsystem.component.overlay.SheetDetent
import com.base.app.core.designsystem.component.overlay.ToastMessage
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun OverlaysSection() {
    CatalogGroup("Alert", caption = "Over everything, the tab bar included. A tap outside does nothing; Back cancels.") {
        var plain by remember { mutableStateOf(false) }
        var destructive by remember { mutableStateOf(false) }
        Wrap {
            AppButton(text = "Alert", onClick = { plain = true }, variant = ButtonVariant.Tinted)
            AppButton(text = "Destructive alert", onClick = { destructive = true }, variant = ButtonVariant.Destructive)
        }
        AppAlert(visible = plain, title = "Allow notifications?", message = "We only send replies and mentions.", confirmLabel = "Allow", dismissLabel = "Not now", onConfirm = { plain = false }, onDismissRequest = { plain = false })
        AppAlert(
            visible = destructive,
            title = "Delete this list?",
            message = "Its 12 items will be deleted too.",
            confirmLabel = "Delete",
            dismissLabel = "Cancel",
            destructive = true,
            icon = AppIcons.Trash,
            onConfirm = { destructive = false },
            onDismissRequest = { destructive = false },
        )
    }
    CatalogGroup("Action sheet", caption = "A choice runs before the sheet asks to close.") {
        var open by remember { mutableStateOf(false) }
        AppButton(text = "Share options", onClick = { open = true }, variant = ButtonVariant.Tinted)
        AppActionSheet(
            visible = open,
            title = "photo.jpg",
            message = "Shared photos are visible to everyone in the album.",
            actions = listOf(
                SheetAction("Copy link", onClick = {}, icon = AppIcons.Copy),
                SheetAction("Save to files", onClick = {}, icon = AppIcons.Download),
                SheetAction("Delete", onClick = {}, icon = AppIcons.Trash, destructive = true),
            ),
            onDismissRequest = { open = false },
        )
    }
    CatalogGroup("Sheet", caption = "Detents, a close button, a footer pinned to the visible bottom, and scrolling that hands off to the sheet at either end.") {
        var medium by remember { mutableStateOf(false) }
        var large by remember { mutableStateOf(false) }
        Wrap {
            AppButton(text = "Medium and large", onClick = { medium = true }, variant = ButtonVariant.Tinted)
            AppButton(text = "Large only", onClick = { large = true }, variant = ButtonVariant.Tinted)
        }
        AppSheet(
            visible = medium,
            onDismissRequest = { medium = false },
            title = "Filters",
            showCloseButton = true,
            footer = { AppBottomActionBar { AppButton(text = "Show 24 results", onClick = { medium = false }, modifier = Modifier.fillMaxWidth()) } },
        ) {
            AppSection { repeat(16) { AppRow(title = "Option ${it + 1}", onClick = {}) } }
        }
        AppSheet(visible = large, onDismissRequest = { large = false }, detents = setOf(SheetDetent.Large), title = "New message", showCloseButton = true) {
            AppText(
                text = "A large-only sheet opens nearly full height.",
                modifier = Modifier.padding(horizontal = AppTheme.spacing.gutter),
                style = AppTheme.typography.body,
            )
        }
    }
    CatalogGroup("Menu", caption = "A pull-down from a button. It grows out of its anchor, keeps off the edges, and closes when an item is chosen.") {
        var pullDown by remember { mutableStateOf(false) }
        Box {
            AppIconButton(AppIcons.MoreHorizontal, contentDescription = "More", onClick = { pullDown = true }, variant = ButtonVariant.Tinted)
            AppMenu(expanded = pullDown, onDismissRequest = { pullDown = false }) {
                AppMenuItem(text = "Rename", icon = AppIcons.Edit, onClick = {})
                AppMenuItem(text = "Duplicate", icon = AppIcons.Copy, onClick = {})
                AppMenuDivider()
                AppMenuItem(text = "Delete", icon = AppIcons.Trash, destructive = true, onClick = {})
                AppMenuItem(text = "Unavailable", onClick = {}, enabled = false)
            }
        }
    }
    CatalogGroup("Context menu", caption = "Long-press a row: it lifts, the page dims and the menu opens beside it. A right click opens it at the pointer.") {
        AppSection {
            listOf("Weekly report", "Invoice 0142", "Holiday photos").forEach { name ->
                AppContextMenu(
                    menu = {
                        AppMenuItem(text = "Share", icon = AppIcons.Share, onClick = {})
                        AppMenuItem(text = "Duplicate", icon = AppIcons.Copy, onClick = {})
                        AppMenuDivider()
                        AppMenuItem(text = "Delete", icon = AppIcons.Trash, destructive = true, onClick = {})
                    },
                ) {
                    AppRow(title = name, subtitle = "Edited today", onClick = {})
                }
            }
        }
    }
    CatalogGroup("Toast", caption = "One at a time, above whatever covers the bottom edge. The same message twice restarts the one showing. Swipe down to dismiss.") {
        val toasts = LocalOverlayHost.current?.toasts
        Wrap {
            AppButton(text = "Plain", onClick = { toasts?.show(ToastMessage(text = "Link copied")) }, variant = ButtonVariant.Tinted)
            AppButton(text = "With undo", onClick = { toasts?.show(ToastMessage(text = "Message archived", actionLabel = "Undo", onAction = {})) }, variant = ButtonVariant.Tinted)
            AppButton(text = "Error", onClick = { toasts?.show(ToastMessage(text = "Could not send", title = "Offline", tone = AppTone.Error)) }, variant = ButtonVariant.Tinted)
            AppButton(text = "HUD", onClick = { toasts?.show(ToastMessage(text = "Saved", tone = AppTone.Success, hud = true)) }, variant = ButtonVariant.Tinted)
        }
        AppToast(ToastMessage(text = "A toast drawn inline", tone = AppTone.Info))
    }
    CatalogGroup("Loading overlay", caption = "Blocks the whole window, tab bar included, from touch, a screen reader and Back.") {
        var busy by remember { mutableStateOf(false) }
        LaunchedEffect(busy) {
            if (busy) {
                delay(2_000)
                busy = false
            }
        }
        AppButton(text = "Block the screen for 2 s", onClick = { busy = true }, variant = ButtonVariant.Tinted)
        AppLoadingOverlay(visible = busy, label = "Uploading")
    }
}
