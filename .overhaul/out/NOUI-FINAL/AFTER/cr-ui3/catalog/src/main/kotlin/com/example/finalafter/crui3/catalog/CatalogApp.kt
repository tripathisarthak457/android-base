package com.example.finalafter.crui3.catalog

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.example.finalafter.crui3.catalog.sections.LoadingScreen
import com.example.finalafter.crui3.core.designsystem.component.container.AppScaffold
import com.example.finalafter.crui3.core.designsystem.component.container.AppVerticalDivider
import com.example.finalafter.crui3.core.designsystem.component.list.AppRow
import com.example.finalafter.crui3.core.designsystem.component.list.AppSection
import com.example.finalafter.crui3.core.designsystem.component.list.AppToggleRow
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.finalafter.crui3.core.designsystem.component.navigation.AppTopBar
import com.example.finalafter.crui3.core.designsystem.component.navigation.scrolledFromTop
import com.example.finalafter.crui3.core.designsystem.component.selection.AppSegmentedControl
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.finalafter.crui3.core.designsystem.foundation.scroll.appScrollIndicator
import com.example.finalafter.crui3.core.designsystem.theme.AppDesignStyle
import com.example.finalafter.crui3.core.designsystem.theme.AppMotionStyle
import com.example.finalafter.crui3.core.designsystem.theme.AppTheme
import com.example.finalafter.crui3.core.designsystem.theme.ThemeMode

/**
 * The catalog's own navigation: one nullable selection. With room for two panes the index stays
 * beside the page it opened; otherwise the page slides over the index on the screen spring, as a
 * pushed screen does in the app, and Back returns.
 */
@Composable
internal fun CatalogApp(initialSection: CatalogSection?, initialEnvironment: CatalogEnvironment, frames: FrameTimes) {
    var environment by rememberSaveable(stateSaver = CatalogEnvironment.Saver) { mutableStateOf(initialEnvironment) }
    var section by rememberSaveable { mutableStateOf(initialSection) }

    ProvideEnvironment(environment) {
        SystemBarIcons(dark = !AppTheme.colors.isLight)
        // An opaque floor under both pages, so the window's own colour never shows mid-transition.
        // Test tags are resource ids here, so device scripts can find what to tap.
        Box(Modifier.fillMaxSize().background(AppTheme.colors.background).semantics { testTagsAsResourceId = true }) {
            val index: @Composable () -> Unit = {
                SectionIndex(environment = environment, onEnvironment = { environment = it }, selected = section, onSelect = { section = it })
            }
            if (AppTheme.containerClass.hasRoomForTwoPanes) {
                val shown = section ?: CatalogSection.entries.first()
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.width(AppTheme.layout.listPaneWidth).fillMaxHeight()) { index() }
                    AppVerticalDivider()
                    Box(Modifier.weight(1f).fillMaxHeight()) { key(shown) { SectionPage(shown, onBack = null) } }
                }
            } else {
                NavigationBackHandler(
                    state = rememberNavigationEventState(NavigationEventInfo.None),
                    isBackEnabled = section != null,
                    onBackCompleted = { section = null },
                )
                val spring = AppTheme.springs.screen.offset
                val reduceMotion = AppTheme.reduceMotion
                val parallax = AppTheme.springs.navigation.parallax
                // The index leaves composition while a page covers it; its scroll position is kept.
                val saved = rememberSaveableStateHolder()
                AnimatedContent(
                    targetState = section,
                    transitionSpec = {
                        val forward = targetState != null
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else if (forward) {
                            slideInHorizontally(spring) { it } togetherWith slideOutHorizontally(spring) { -(it * parallax).toInt() }
                        } else {
                            slideInHorizontally(spring) { -(it * parallax).toInt() } togetherWith slideOutHorizontally(spring) { it }
                        }
                    },
                    contentKey = { it?.name },
                    label = "catalogPage",
                ) { current ->
                    Box(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
                        saved.SaveableStateProvider(current?.name ?: IndexKey) {
                            if (current == null) index() else SectionPage(current, onBack = { section = null })
                        }
                    }
                }
            }
            // Bottom-leading, where no page puts a title or a bar action for it to cover.
            FrameTimesReadout(frames, Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 8.dp))
        }
    }
}

/**
 * Status and navigation bar icons follow the catalog's theme rather than the system's: a page shown
 * dark on a light phone would otherwise draw dark icons on a dark screen. Applied while the frame
 * that first draws the theme is composed, so no frame shows the wrong icons.
 */
@Composable
private fun SystemBarIcons(dark: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    SideEffect(activity, dark) {
        val style = if (dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        activity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

/** One section's groups, in as many columns as the pane has room for. */
@Composable
private fun SectionPage(section: CatalogSection, onBack: (() -> Unit)?) {
    // A screen with its own title and list, as an app has, so it is measured as an app would be.
    if (section == CatalogSection.Loading) {
        LoadingScreen(onBack)
        return
    }
    val scroll = rememberScrollState()
    AppScaffold(topBar = { AppTopBar(title = section.title, onBack = onBack, scrolled = { scroll.value }) }) {
        MasonryColumns(
            minColumnWidth = GroupMinWidth,
            spacing = AppTheme.spacing.section,
            modifier = Modifier
                .fillMaxSize()
                .appScrollIndicator(scroll)
                .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                .padding(catalogContentPadding(PaddingValues(AppTheme.spacing.gutter))),
        ) {
            section.Content()
        }
    }
}

@Composable
private fun SectionIndex(
    environment: CatalogEnvironment,
    onEnvironment: (CatalogEnvironment) -> Unit,
    selected: CatalogSection?,
    onSelect: (CatalogSection) -> Unit,
) {
    val list = rememberLazyListState()
    AppScaffold(
        background = AppTheme.colors.backgroundGrouped,
        topBar = { AppLargeTitleTopBar(title = "Catalog", subtitle = "Every component, in this project's style", scrolled = { list.scrolledFromTop() }) },
    ) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().appScrollIndicator(list),
            flingBehavior = AppScrollDefaults.flingBehavior(),
            contentPadding = catalogContentPadding(PaddingValues(bottom = AppTheme.spacing.xl)),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
        ) {
            item(key = "environment") { EnvironmentBar(environment, onEnvironment) }
            item(key = "sections") {
                AppSection(header = "Sections") {
                    CatalogSection.entries.forEach { entry ->
                        AppRow(
                            title = entry.title,
                            subtitle = entry.summary,
                            value = if (entry == selected) "Open" else null,
                            onClick = { onSelect(entry) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Everything a page is seen in, chosen once here, so every page below is seen the same way and a
 * mismatch between two components shows up as you browse.
 */
@Composable
private fun EnvironmentBar(environment: CatalogEnvironment, onChange: (CatalogEnvironment) -> Unit) {
    AppSection(header = "Seen in", footer = "Starts in the style and motion this project was generated with.") {
        Column(Modifier.padding(AppTheme.spacing.md), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            AppSegmentedControl(
                options = ThemeMode.entries.map { it.name },
                selected = environment.themeMode.ordinal,
                onSelect = { onChange(environment.copy(themeMode = ThemeMode.entries[it])) },
            )
            AppSegmentedControl(
                options = AppDesignStyle.entries.map { it.name },
                selected = environment.designStyle.ordinal,
                onSelect = { onChange(environment.copy(designStyle = AppDesignStyle.entries[it])) },
            )
            AppSegmentedControl(
                options = AppMotionStyle.entries.map { it.name },
                selected = environment.motionStyle.ordinal,
                onSelect = { onChange(environment.copy(motionStyle = AppMotionStyle.entries[it])) },
            )
            AppSegmentedControl(
                options = CatalogEnvironment.FontScales.map { "${it}x" },
                selected = CatalogEnvironment.FontScales.indexOf(environment.fontScale).coerceAtLeast(0),
                onSelect = { onChange(environment.copy(fontScale = CatalogEnvironment.FontScales[it])) },
            )
        }
        AppToggleRow(title = "Right to left", checked = environment.rtl, onCheckedChange = { onChange(environment.copy(rtl = it)) })
        AppToggleRow(title = "Reduce motion", checked = environment.reduceMotion, onCheckedChange = { onChange(environment.copy(reduceMotion = it)) })
    }
}

private val GroupMinWidth = 340.dp
private const val IndexKey = "index"
