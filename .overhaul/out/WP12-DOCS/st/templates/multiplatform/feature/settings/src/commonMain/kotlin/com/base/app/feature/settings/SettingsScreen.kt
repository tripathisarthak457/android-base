package com.base.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.base.app.core.datastore.settings.AppSettings
import com.base.app.core.designsystem.component.list.AppChoiceRow
import com.base.app.core.designsystem.component.list.AppRow
import com.base.app.core.designsystem.component.list.AppSection
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.feature.settings.resources.Res
import com.base.app.feature.settings.resources.settings_about
import com.base.app.feature.settings.resources.settings_appearance
import com.base.app.feature.settings.resources.settings_theme_dark
import com.base.app.feature.settings.resources.settings_theme_explanation
import com.base.app.feature.settings.resources.settings_theme_light
import com.base.app.feature.settings.resources.settings_theme_system
import com.base.app.feature.settings.resources.settings_title
import com.base.app.feature.settings.resources.settings_version
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
// <opt:language>
import com.base.app.feature.settings.resources.settings_language
import com.base.app.feature.settings.resources.settings_language_explanation
// </opt:language>

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    // <opt:language>
    languageSettingsAvailable: Boolean = true,
    // </opt:language>
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.backgroundGrouped)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
    ) {
        AppLargeTitle(title = stringResource(Res.string.settings_title))

        val sectionPadding = Modifier.padding(horizontal = AppTheme.spacing.gutter)
        AppSection(
            modifier = sectionPadding,
            header = stringResource(Res.string.settings_appearance),
            footer = stringResource(Res.string.settings_theme_explanation),
        ) {
            ThemeModes.forEach { (mode, label) ->
                AppChoiceRow(
                    title = stringResource(label),
                    selected = state.themeMode == mode,
                    onSelect = { onEvent(SettingsEvent.ThemeSelected(mode)) },
                )
            }
        }

        // <opt:language>
        if (languageSettingsAvailable) {
            AppSection(
                modifier = sectionPadding,
                footer = stringResource(Res.string.settings_language_explanation),
            ) {
                AppRow(
                    title = stringResource(Res.string.settings_language),
                    onClick = { onEvent(SettingsEvent.LanguageClicked) },
                )
            }
        }
        // </opt:language>

        AppSection(modifier = sectionPadding, header = stringResource(Res.string.settings_about)) {
            AppRow(title = stringResource(Res.string.settings_version), value = state.appVersion)
        }
    }
}

private val ThemeModes: List<Pair<String, StringResource>> = listOf(
    AppSettings.THEME_SYSTEM to Res.string.settings_theme_system,
    AppSettings.THEME_LIGHT to Res.string.settings_theme_light,
    AppSettings.THEME_DARK to Res.string.settings_theme_dark,
)

@Preview
@Composable
private fun SettingsPreview() {
    AppTheme {
        SettingsScreen(state = SettingsState(appVersion = "1.0.0-devDebug"), onEvent = {})
    }
}
