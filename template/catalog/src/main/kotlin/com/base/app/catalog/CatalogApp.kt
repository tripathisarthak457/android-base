package com.base.app.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.container.AppVerticalDivider
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.selection.AppChip
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppDesignStyle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode

/**
 * The catalog's own navigation: one nullable selection. With room for two panes the index stays
 * beside the page it opened; otherwise the page replaces it and Back returns.
 */
@Composable
fun CatalogApp() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
    var section by rememberSaveable { mutableStateOf<CatalogSection?>(null) }
    var designStyle by rememberSaveable { mutableStateOf(AppDesignStyle.Utility) }

    AppTheme(mode = themeMode, designStyle = designStyle) {
        val index: @Composable (CatalogSection?) -> Unit = { selected ->
            SectionIndex(
                themeMode = themeMode,
                designStyle = designStyle,
                selected = selected,
                onDesignStyle = { designStyle = it },
                onToggleTheme = { themeMode = themeMode.next() },
                onSelect = { section = it },
            )
        }

        if (AppTheme.windowSize.hasRoomForTwoPanes) {
            val shown = section ?: CatalogSection.entries.first()
            Row(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.width(AppTheme.layout.listPaneWidth).fillMaxHeight()) { index(shown) }
                AppVerticalDivider()
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    key(shown) { SectionPage(section = shown, onBack = null) }
                }
            }
            return@AppTheme
        }

        BackHandler(enabled = section != null) { section = null }

        // Captured out of the transitionSpec: it is not a composable scope, so the theme's
        // composable accessors cannot be read from inside it.
        val enterDuration = AppTheme.motion.medium
        val exitDuration = AppTheme.motion.quick

        AnimatedContent(
            targetState = section,
            transitionSpec = {
                fadeIn(tween(enterDuration)) togetherWith fadeOut(tween(exitDuration))
            },
            label = "catalogSection",
        ) { current ->
            if (current == null) index(null) else SectionPage(section = current, onBack = { section = null })
        }
    }
}

/** One section's groups, in as many columns as the pane has room for. */
@Composable
private fun SectionPage(section: CatalogSection, onBack: (() -> Unit)?) {
    AppScaffold(
        topBar = {
            if (onBack != null) {
                AppBackTopBar(title = section.title, onBack = onBack)
            } else {
                AppLargeTitle(title = section.title, subtitle = section.summary)
            }
        },
    ) {
        MasonryColumns(
            minColumnWidth = GROUP_MIN_WIDTH,
            spacing = AppTheme.spacing.section,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.spacing.gutter),
        ) {
            section.Content()
        }
    }
}

private val GROUP_MIN_WIDTH = 340.dp

@Composable
private fun SectionIndex(
    themeMode: ThemeMode,
    designStyle: AppDesignStyle,
    selected: CatalogSection?,
    onDesignStyle: (AppDesignStyle) -> Unit,
    onToggleTheme: () -> Unit,
    onSelect: (CatalogSection) -> Unit,
) {
    AppScaffold(
        topBar = {
            AppLargeTitle(
                title = "Catalog",
                subtitle = "Every component, in both themes and all four styles",
                actions = {
                    AppIconButton(
                        icon = when (themeMode) {
                            ThemeMode.Light -> AppIcons.Sun
                            ThemeMode.Dark -> AppIcons.Moon
                            ThemeMode.System -> AppIcons.Settings
                        },
                        contentDescription = "Theme: ${themeMode.name}. Tap to change.",
                        onClick = onToggleTheme,
                    )
                },
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = AppTheme.spacing.xxl),
        ) {
            // The style is chosen here rather than per page, so every page below is seen in the
            // same one and a mismatch between two components shows up as you browse.
            item {
                FlowRow(
                    modifier = Modifier.padding(
                        horizontal = AppTheme.spacing.gutter,
                        vertical = AppTheme.spacing.sm,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    AppDesignStyle.entries.forEach { option ->
                        AppChip(
                            label = option.name,
                            selected = option == designStyle,
                            onClick = { onDesignStyle(option) },
                        )
                    }
                }
            }
            items(CatalogSection.entries.size) { index ->
                val entry = CatalogSection.entries[index]
                AppListItem(
                    title = entry.title,
                    supporting = entry.summary,
                    modifier = if (entry == selected) {
                        Modifier.background(AppTheme.colors.accentSubtle)
                    } else {
                        Modifier
                    },
                    onClick = { onSelect(entry) },
                    trailing = {
                        AppIcon(
                            AppIcons.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                )
            }
        }
    }
}

private fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.System -> ThemeMode.Light
    ThemeMode.Light -> ThemeMode.Dark
    ThemeMode.Dark -> ThemeMode.System
}
