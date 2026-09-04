package com.base.app.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.base.app.core.designsystem.component.button.AppIconButton
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.navigation.AppBackTopBar
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.designsystem.theme.ThemeMode

/**
 * The catalog's own navigation: one nullable selection, and a system back handler.
 *
 * Deliberately not the app's navigation stack. The catalog depends on `:core:designsystem` alone,
 * and pulling in `:core:navigation` to move between eleven static pages would defeat the entire
 * point of keeping this module's rebuild cheap.
 *
 * The theme toggle is the reason anyone opens this app twice: every component below has a dark
 * variant, and the only way to know they all work is to flip between them on a real screen.
 */
@Composable
fun CatalogApp() {
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.System) }
    var section by rememberSaveable { mutableStateOf<CatalogSection?>(null) }

    AppTheme(mode = themeMode) {
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
            if (current == null) {
                SectionIndex(
                    themeMode = themeMode,
                    onToggleTheme = { themeMode = themeMode.next() },
                    onSelect = { section = it },
                )
            } else {
                AppScaffold(
                    topBar = {
                        AppBackTopBar(
                            title = current.title,
                            onBack = { section = null },
                        )
                    },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(AppTheme.spacing.gutter),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
                    ) {
                        current.Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionIndex(
    themeMode: ThemeMode,
    onToggleTheme: () -> Unit,
    onSelect: (CatalogSection) -> Unit,
) {
    AppScaffold(
        topBar = {
            AppLargeTitle(
                title = "Catalog",
                subtitle = "Every component, in both themes",
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
            items(CatalogSection.entries.size) { index ->
                val entry = CatalogSection.entries[index]
                AppListItem(
                    title = entry.title,
                    supporting = entry.summary,
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
