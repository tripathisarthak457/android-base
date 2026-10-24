package com.example.deskstandard.catalog.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.deskstandard.catalog.CatalogGroup
import com.example.deskstandard.core.designsystem.component.button.AppIconButton
import com.example.deskstandard.core.designsystem.component.container.AppCard
import com.example.deskstandard.core.designsystem.component.container.AppScaffold
import com.example.deskstandard.core.designsystem.component.input.AppSearchField
import com.example.deskstandard.core.designsystem.component.list.AppRow
import com.example.deskstandard.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.deskstandard.core.designsystem.component.navigation.AppPageControl
import com.example.deskstandard.core.designsystem.component.navigation.AppPager
import com.example.deskstandard.core.designsystem.component.navigation.AppTabRow
import com.example.deskstandard.core.designsystem.component.navigation.AppTopBar
import com.example.deskstandard.core.designsystem.component.navigation.scrolledFromTop
import com.example.deskstandard.core.designsystem.component.text.AppText
import com.example.deskstandard.core.designsystem.foundation.layout.chromeContentPadding
import com.example.deskstandard.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.deskstandard.core.designsystem.foundation.scroll.edgeProtection
import com.example.deskstandard.core.designsystem.icon.AppIcons
import com.example.deskstandard.core.designsystem.theme.AppTheme

@Composable
fun BarsSection() {
    CatalogGroup("Inline top bar", caption = "The title takes what the buttons leave, so it never runs under a second action.") {
        AppTopBar(title = "Messages", onBack = {}, windowInsets = WindowInsets(0)) {
            AppIconButton(AppIcons.Search, contentDescription = "Search", onClick = {})
            AppIconButton(AppIcons.Edit, contentDescription = "Compose", onClick = {})
        }
        AppTopBar(
            title = "A title that is far too long for the room it has",
            subtitle = "And a subtitle",
            centerTitle = false,
            windowInsets = WindowInsets(0),
        ) {
            AppIconButton(AppIcons.MoreHorizontal, contentDescription = "More", onClick = {})
        }
    }
    CatalogGroup(
        "Large title",
        caption = "Scroll the list: the title and its search field rise into the bar, the small title fades in and the bar's " +
            "surface appears under it. Only drawing changes.",
    ) {
        val list = rememberLazyListState()
        val search = rememberTextFieldState()
        Box(Modifier.fillMaxWidth().height(420.dp)) {
            AppScaffold(
                topBar = {
                    AppLargeTitleTopBar(
                        title = "Inbox",
                        scrolled = { list.scrolledFromTop() },
                        windowInsets = WindowInsets(0),
                        accessory = { AppSearchField(state = search) },
                    ) { AppIconButton(AppIcons.Edit, contentDescription = "Compose", onClick = {}) }
                },
            ) {
                LazyColumn(
                    state = list,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = chromeContentPadding(),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                ) {
                    items((1..30).toList(), key = { it }) { AppRow(title = "Message $it", subtitle = "Preview of the message", onClick = {}) }
                }
            }
        }
    }
    CatalogGroup(
        "Edge protection",
        caption = "Content fades out under a clear bar, so the bar needs no background: the page colour drawn over it as a " +
            "gradient, with no offscreen layer. Over glass or an image, scrollEdgeFade fades the content itself.",
    ) {
        val list = rememberLazyListState()
        val barHeight = 48.dp
        val background = AppTheme.colors.background
        Box(Modifier.fillMaxWidth().height(240.dp).background(background)) {
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxSize().edgeProtection({ background }, top = { barHeight }),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) {
                items((1..30).toList(), key = { it }) { AppRow(title = "Row $it") }
            }
            Box(Modifier.fillMaxWidth().height(barHeight), contentAlignment = Alignment.Center) {
                AppText(text = "Clear bar", style = AppTheme.typography.headline)
            }
        }
    }
    CatalogGroup("Tab row", caption = "Labels share the width while they fit, and take what they need when they do not.") {
        var tab by remember { mutableIntStateOf(0) }
        AppTabRow(tabs = listOf("Overview", "Activity", "Settings"), selected = tab, onSelect = { tab = it })
        var long by remember { mutableIntStateOf(0) }
        AppTabRow(tabs = listOf("All", "Exceptions 12", "Pending", "Done"), selected = long, onSelect = { long = it })
    }
    CatalogGroup("Pager and page control", caption = "Tap a side of the dots to step, or drag along them to scrub.") {
        AppPager(pageCount = 4, modifier = Modifier.height(160.dp)) { page ->
            AppCard(modifier = Modifier.fillMaxSize()) { AppText(text = "Page ${page + 1}", style = AppTheme.typography.title3) }
        }
        val pager = rememberPagerState { 6 }
        AppPageControl(pageCount = 6, selected = { pager.currentPage })
    }
}
