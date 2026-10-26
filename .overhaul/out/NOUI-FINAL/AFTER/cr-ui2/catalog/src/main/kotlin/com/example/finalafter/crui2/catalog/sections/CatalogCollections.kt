package com.example.finalafter.crui2.catalog.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui2.catalog.CatalogGroup
import com.example.finalafter.crui2.core.designsystem.component.button.AppButton
import com.example.finalafter.crui2.core.designsystem.component.button.ButtonVariant
import com.example.finalafter.crui2.core.designsystem.component.collections.AppCarousel
import com.example.finalafter.crui2.core.designsystem.component.collections.AppImageViewer
import com.example.finalafter.crui2.core.designsystem.component.collections.AppReorderableList
import com.example.finalafter.crui2.core.designsystem.component.container.AppCard
import com.example.finalafter.crui2.core.designsystem.component.list.AppRow
import com.example.finalafter.crui2.core.designsystem.component.text.AppIcon
import com.example.finalafter.crui2.core.designsystem.component.text.AppText
import com.example.finalafter.crui2.core.designsystem.icon.AppIcons
import com.example.finalafter.crui2.core.designsystem.theme.AppTheme

@Composable
fun CollectionsSection() {
    CatalogGroup("Carousel", caption = "Cards snap one at a time with the next one peeking in.") {
        AppCarousel(pageCount = 5, modifier = Modifier.height(180.dp)) { page ->
            AppCard(modifier = Modifier.fillMaxSize()) { AppText(text = "Card ${page + 1}", style = AppTheme.typography.title3) }
        }
    }
    CatalogGroup("Reorderable list", caption = "Press and hold a row, then drag it. It lifts with a tick and the list scrolls near the edges.") {
        var items by remember { mutableStateOf((1..8).map { "Task $it" }) }
        AppReorderableList(
            items = items,
            key = { it },
            onMove = { from, to -> items = items.toMutableList().apply { add(to, removeAt(from)) } },
            modifier = Modifier.fillMaxWidth().height(320.dp),
        ) { item, lifted ->
            AppRow(title = item, modifier = Modifier.background(if (lifted) AppTheme.colors.surfaceElevated else AppTheme.colors.surface), leading = { AppIcon(AppIcons.Menu, contentDescription = null) })
        }
    }
    CatalogGroup("Image viewer", caption = "Pinch or double-tap to zoom, drag to look around, swipe down to close. In an app it is a full-screen cover; here it is boxed.") {
        var open by remember { mutableStateOf(true) }
        if (open) {
            Box(Modifier.fillMaxWidth().height(360.dp)) {
                AppImageViewer(onDismiss = { open = false }) {
                    Box(Modifier.size(200.dp).background(AppTheme.colors.accent), contentAlignment = Alignment.Center) {
                        AppIcon(AppIcons.ImageIcon, contentDescription = null, tint = AppTheme.colors.onAccent, size = 64.dp)
                    }
                }
            }
        } else {
            AppButton(text = "Show again", onClick = { open = true }, variant = ButtonVariant.Tinted)
        }
    }
}
