package com.base.app.catalog.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.catalog.CatalogGroup
import com.base.app.core.designsystem.component.feedback.AppAvatar
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.list.AppChoiceRow
import com.base.app.core.designsystem.component.list.AppDisclosure
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.list.AppSection
import com.base.app.core.designsystem.component.list.AppSectionHeader
import com.base.app.core.designsystem.component.list.AppSwipeActions
import com.base.app.core.designsystem.component.list.AppToggleRow
import com.base.app.core.designsystem.component.list.SwipeAction
import com.base.app.core.designsystem.component.refresh.AppPullToRefresh
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.foundation.draw.HairlineEdge
import com.base.app.core.designsystem.foundation.draw.groupItem
import com.base.app.core.designsystem.foundation.draw.hairline
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun ListsSection() {
    CatalogGroup("Inset-grouped section", caption = "Rows share one rounded group; each hairline starts where the next row's text does.") {
        Box(Modifier.background(AppTheme.colors.backgroundGrouped)) {
            var wifi by remember { mutableStateOf(true) }
            AppSection(header = "Account", footer = "Rows grow with the font size instead of clipping.") {
                AppRow(title = "Profile", leading = { AppAvatar(name = "Ada Lovelace", size = 32.dp) }, onClick = {})
                AppRow(title = "Language", value = "English", onClick = {})
                AppRow(title = "Notifications", subtitle = "Mentions and replies", leading = { AppIcon(AppIcons.Bell, contentDescription = null) }, onClick = {})
                AppToggleRow(title = "Wi-Fi only", checked = wifi, onCheckedChange = { wifi = it }, leading = { AppIcon(AppIcons.Wifi, contentDescription = null) })
                AppRow(title = "Plain row")
                AppRow(title = "Disabled", onClick = {}, enabled = false)
                AppRow(title = "Sign out", destructive = true, onClick = {})
            }
        }
    }
    CatalogGroup("Choice rows", caption = "One of several, as Settings lists a language or a sort order: the chosen row carries the checkmark.") {
        var choice by remember { mutableIntStateOf(0) }
        AppSection {
            listOf("Automatic", "Light", "Dark").forEachIndexed { index, label ->
                AppChoiceRow(title = label, selected = choice == index, onSelect = { choice = index })
            }
        }
    }
    CatalogGroup("In a lazy list", caption = "A list too long for one section draws the group from its rows: groupItem shapes and a hairline per row.") {
        val shape = AppTheme.shapes.md
        Column(Modifier.background(AppTheme.colors.backgroundGrouped)) {
            AppSectionHeader(title = "Recent", actionLabel = "See all", onAction = {})
            val rows = listOf("Groceries", "Rent", "Coffee")
            rows.forEachIndexed { index, title ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.surfaceGrouped, shape.groupItem(first = index == 0, last = index == rows.lastIndex))
                        .then(if (index < rows.lastIndex) Modifier.hairline(HairlineEdge.Bottom, startInset = AppTheme.spacing.lg) else Modifier),
                ) {
                    AppRow(title = title, value = "-$12.00", onClick = {})
                }
            }
        }
    }
    CatalogGroup("Disclosure", caption = "Its chevron turns to point at what it opened; a screen reader hears whether it is expanded.") {
        var open by remember { mutableStateOf(false) }
        AppDisclosure(title = "Shipping details", subtitle = "2 items", expanded = open, onExpandedChange = { open = it }) {
            AppText(text = "Arrives Thursday. Leave with a neighbour if nobody is home.", style = AppTheme.typography.subheadline)
        }
    }
    CatalogGroup("Swipe actions", caption = "Swipe either way; past most of the width the outermost action runs. A tap on an open row closes it.") {
        AppSwipeActions(
            leading = listOf(SwipeAction("Read", AppIcons.Check, onClick = {}, tone = AppTone.Info)),
            trailing = listOf(SwipeAction("Delete", AppIcons.Trash, onClick = {}, destructive = true), SwipeAction("Archive", AppIcons.Folder, onClick = {})),
            shape = AppTheme.shapes.md,
        ) {
            AppRow(title = "Swipe me", subtitle = "Either direction", onClick = {})
        }
    }
    CatalogGroup("Pull to refresh", caption = "The content follows the finger; the spokes appear as the pull nears the point where it refreshes.") {
        var refreshing by remember { mutableStateOf(false) }
        LaunchedEffect(refreshing) {
            if (refreshing) {
                delay(1_500)
                refreshing = false
            }
        }
        AppPullToRefresh(isRefreshing = refreshing, onRefresh = { refreshing = true }, modifier = Modifier.fillMaxWidth().height(240.dp)) {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed((1..20).toList(), key = { _, it -> it }) { _, item -> AppRow(title = "Item $item") }
            }
        }
    }
}
