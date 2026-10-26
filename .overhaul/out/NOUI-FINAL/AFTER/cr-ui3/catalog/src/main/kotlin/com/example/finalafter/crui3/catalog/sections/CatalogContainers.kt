package com.example.finalafter.crui3.catalog.sections

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.catalog.CatalogGroup
import com.example.finalafter.crui3.core.designsystem.component.button.AppButton
import com.example.finalafter.crui3.core.designsystem.component.container.AppBottomActionBar
import com.example.finalafter.crui3.core.designsystem.component.container.AppCard
import com.example.finalafter.crui3.core.designsystem.component.container.AppDivider
import com.example.finalafter.crui3.core.designsystem.component.container.AppScaffold
import com.example.finalafter.crui3.core.designsystem.component.container.AppVerticalDivider
import com.example.finalafter.crui3.core.designsystem.component.list.AppRow
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppTopBar
import com.example.finalafter.crui3.core.designsystem.component.text.AppText
import com.example.finalafter.crui3.core.designsystem.foundation.adaptive.AppTwoPane
import com.example.finalafter.crui3.core.designsystem.foundation.content.AppSurface
import com.example.finalafter.crui3.core.designsystem.foundation.layout.chromeContentPadding
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme

@Composable
internal fun ContainersSection() {
    CatalogGroup("Card", caption = "The style decides the treatment: outlined, raised on the elevated surface, or offset.") {
        AppCard(modifier = Modifier.fillMaxWidth()) {
            AppText(text = "A static card", style = AppTheme.typography.headline)
            AppText(text = "For a block of related content.", style = AppTheme.typography.subheadline, color = AppTheme.colors.contentSecondary)
        }
        AppCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            AppText(text = "A card that opens something", style = AppTheme.typography.headline)
        }
        AppCard(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            AppText(text = "A disabled card", style = AppTheme.typography.headline)
        }
    }
    CatalogGroup("Surface and dividers", caption = "Hairlines are one physical pixel.") {
        AppSurface(modifier = Modifier.fillMaxWidth().height(56.dp), color = AppTheme.colors.surfaceElevated, elevation = AppTheme.elevation.overlay, contentAlignment = Alignment.Center) {
            AppText(text = "AppSurface, elevated", style = AppTheme.typography.subheadline)
        }
        AppDivider()
        AppDivider(startIndent = AppTheme.spacing.lg)
        Row(Modifier.height(32.dp)) {
            AppText(text = "Left", style = AppTheme.typography.subheadline)
            AppVerticalDivider(Modifier.padding(horizontal = AppTheme.spacing.md))
            AppText(text = "Right", style = AppTheme.typography.subheadline)
        }
    }
    CatalogGroup("Scaffold", caption = "Content runs under the top bar and above the bottom action bar; the list pads itself with chromeContentPadding().") {
        Box(Modifier.fillMaxWidth().height(320.dp)) {
            AppScaffold(
                topBar = { AppTopBar(title = "Checkout", onBack = {}, windowInsets = WindowInsets(0)) },
                bottomBar = { AppBottomActionBar { AppButton(text = "Pay $24.00", onClick = {}, modifier = Modifier.fillMaxWidth()) } },
            ) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = chromeContentPadding(),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                ) {
                    items((1..12).toList(), key = { it }) { AppRow(title = "Line item $it", value = "$2.00") }
                }
            }
        }
    }
    CatalogGroup("Two panes", caption = "Two halves of one screen: side by side when the window has room, stacked when it does not.") {
        AppTwoPane(
            first = { AppCard(modifier = Modifier.fillMaxWidth()) { AppText(text = "First", style = AppTheme.typography.headline) } },
            second = { AppCard(modifier = Modifier.fillMaxWidth()) { AppText(text = "Second", style = AppTheme.typography.headline) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
