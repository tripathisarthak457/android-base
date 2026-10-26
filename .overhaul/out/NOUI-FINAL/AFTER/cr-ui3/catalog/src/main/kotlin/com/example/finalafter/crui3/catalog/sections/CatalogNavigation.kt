package com.example.finalafter.crui3.catalog.sections

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.finalafter.crui3.catalog.CatalogGroup
import com.example.finalafter.crui3.catalog.R
import com.example.finalafter.crui3.core.common.navigation.AppNavKey
import com.example.finalafter.crui3.core.designsystem.component.button.AppButton
import com.example.finalafter.crui3.core.designsystem.component.container.AppCard
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppFloatingToolbar
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppSidebar
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppTabBar
import com.example.finalafter.crui3.core.designsystem.component.navigation.BarMinimize
import com.example.finalafter.crui3.core.designsystem.component.navigation.BarOrientation
import com.example.finalafter.crui3.core.designsystem.component.navigation.TabItem
import com.example.finalafter.crui3.core.designsystem.component.navigation.TabLabels
import com.example.finalafter.crui3.core.designsystem.component.navigation.rememberAppTabBarState
import com.example.finalafter.crui3.core.designsystem.component.text.AppText
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui3.core.designsystem.icon.AppIcons
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crui3.core.designsystem.theme.BarTreatment
import com.example.finalafter.crui3.core.navigation.AppNavigator
import com.example.finalafter.crui3.core.navigation.NavGraphEntry
import com.example.finalafter.crui3.core.navigation.NavKeySerialization
import com.example.finalafter.crui3.core.navigation.NavRegistry
import com.example.finalafter.crui3.core.navigation.Presentation
import com.example.finalafter.crui3.core.navigation.navGraph
import com.example.finalafter.crui3.core.navigation.shell.AppShell
import com.example.finalafter.crui3.core.navigation.shell.ShellTab
import com.example.finalafter.crui3.core.navigation.shell.rememberShellState
import kotlinx.serialization.Serializable

/** The tab bar in every variant, and the app's own shell running push, pop, cover and back. */
@Composable
internal fun NavigationSection() {
    CatalogGroup(
        title = "Shell",
        caption = "The app's own shell, transitions and back handling. Push a row, swipe back from the " +
            "edge, press Back quickly, open a cover over a pushed screen.",
    ) {
        Frame(height = 560) { DemoShell() }
    }

    BarTreatment.entries.forEach { treatment ->
        CatalogGroup(title = "Tab bar: ${treatment.name}") {
            TabLabels.entries.forEach { labels ->
                DemoBar(treatment = treatment, labels = labels)
            }
        }
    }

    CatalogGroup(title = "Badges, a trailing action and an accessory") {
        DemoBar(
            items = DemoItems.mapIndexed { index, item -> if (index == 2) item.copy(badgeCount = 12) else item },
            trailing = true,
            accessory = true,
        )
    }

    CatalogGroup(title = "Minimize on scroll", caption = "Scroll the list down; scroll up or tap the bar to open it.") {
        Frame(height = 420) {
            val state = rememberAppTabBarState()
            var selected by remember { mutableIntStateOf(0) }
            LazyColumn(
                modifier = Modifier.fillMaxSize().nestedScroll(state.nestedScrollConnection),
                contentPadding = PaddingValues(AppTheme.spacing.lg),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                flingBehavior = AppScrollDefaults.flingBehavior(),
            ) {
                items(items = (1..40).toList(), key = { it }, contentType = { DemoRow }) { row -> AppText("Row $row") }
            }
            AppTabBar(
                items = DemoItems,
                selected = selected,
                onSelect = { selected = it },
                onReselect = {},
                modifier = Modifier.align(Alignment.BottomCenter),
                treatment = BarTreatment.Floating,
                minimize = BarMinimize.OnScrollDown,
                state = state,
            )
        }
    }

    CatalogGroup(title = "Rail", caption = "The same tabs beside wide windows.") {
        Frame(height = 380) {
            var selected by remember { mutableIntStateOf(0) }
            AppTabBar(
                items = DemoItems,
                selected = selected,
                onSelect = { selected = it },
                onReselect = {},
                orientation = BarOrientation.Vertical,
            )
        }
    }

    CatalogGroup(
        title = "Sidebar",
        caption = "The same tabs as a sidebar, as the shell shows them beside a large window.",
    ) {
        Frame(height = 380) {
            var selected by remember { mutableIntStateOf(0) }
            AppSidebar(
                items = DemoItems,
                selected = selected,
                onSelect = { selected = it },
                onReselect = {},
            )
        }
    }

    CatalogGroup(title = "Floating toolbar", caption = "Actions with the bar's look, for screens without tabs.") {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AppFloatingToolbar {
                Action(AppIcons.Edit, "Edit", onClick = {})
                Action(AppIcons.Share, "Share", onClick = {})
                Action(AppIcons.Trash, "Delete", onClick = {})
            }
        }
    }
}

@Composable
private fun DemoBar(
    treatment: BarTreatment = AppTheme.style.bar,
    labels: TabLabels = TabLabels.Always,
    items: List<TabItem> = DemoItems,
    trailing: Boolean = false,
    accessory: Boolean = false,
) {
    var selected by remember { mutableIntStateOf(0) }
    Box(Modifier.consumeWindowInsets(WindowInsets.safeDrawing)) {
        AppTabBar(
            items = items,
            selected = selected,
            onSelect = { selected = it },
            onReselect = {},
            treatment = treatment,
            labels = labels,
            trailingAction = if (trailing) {
                { Action(AppIcons.Search, "Search", onClick = {}) }
            } else {
                null
            },
            accessory = if (accessory) {
                { AppText("Now playing", modifier = Modifier.padding(AppTheme.spacing.lg)) }
            } else {
                null
            },
        )
    }
}

/** A phone-sized window inside the page, with no system bars of its own. */
@Composable
private fun Frame(height: Int, content: @Composable BoxScope.() -> Unit) {
    val shape = AppTheme.shapes.lg
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(shape)
            .border(AppTheme.style.borderWidth, AppTheme.colors.outline, shape)
            .consumeWindowInsets(WindowInsets.safeDrawing),
        content = content,
    )
}

@Composable
private fun DemoShell() {
    val navigator = remember { AppNavigator() }
    val graphs = remember(navigator) { demoGraphs(navigator) }
    val registry = remember(graphs) { NavRegistry(graphs) }
    val serialization = remember(graphs) { NavKeySerialization(graphs) }
    val state = rememberShellState(tabs = DemoTabs, registry = registry, serialization = serialization)
    AppShell(tabs = DemoTabs, state = state, registry = registry, navigator = navigator)
}

private fun demoGraphs(navigator: AppNavigator): Set<NavGraphEntry> =
    setOf(
        navGraph {
            entry<DemoList> {
                LazyColumn(
                    contentPadding = PaddingValues(AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    flingBehavior = AppScrollDefaults.flingBehavior(),
                ) {
                    items(items = (1..20).toList(), key = { it }, contentType = { DemoRow }) { row ->
                        AppCard(onClick = { navigator.navigate(DemoDetail(row)) }, modifier = Modifier.fillMaxWidth()) {
                            AppText("Row $row")
                        }
                    }
                }
            }
            entry<DemoDetail> { key ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppText("Detail ${key.row}", style = AppTheme.typography.headline)
                    AppButton(text = "Push the next one", onClick = { navigator.navigate(DemoDetail(key.row + 1)) })
                    AppButton(text = "Open a cover", onClick = { navigator.navigate(DemoCover) })
                    AppButton(text = "Back", onClick = navigator::navigateUp)
                }
            }
            entry<DemoCover>(presentation = Presentation.Cover) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(AppTheme.spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppText("A cover, over the bar", style = AppTheme.typography.headline)
                    AppButton(text = "Close", onClick = navigator::navigateUp)
                }
            }
            entry<DemoSettings> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppText("Second tab") }
            }
        },
    )

@Serializable
private data object DemoList : AppNavKey

@Serializable
private data class DemoDetail(val row: Int) : AppNavKey

@Serializable
private data object DemoCover : AppNavKey

@Serializable
private data object DemoSettings : AppNavKey

private val DemoTabs = listOf(
    ShellTab(DemoList, R.string.catalog_demo_list, AppIcons.Home),
    ShellTab(DemoSettings, R.string.catalog_demo_settings, AppIcons.Settings),
)

private val DemoItems = listOf(
    TabItem("Home", AppIcons.Home),
    TabItem("Search", AppIcons.Search),
    TabItem("Alerts", AppIcons.Bell),
    TabItem("Profile", AppIcons.User),
)

private const val DemoRow = "row"
