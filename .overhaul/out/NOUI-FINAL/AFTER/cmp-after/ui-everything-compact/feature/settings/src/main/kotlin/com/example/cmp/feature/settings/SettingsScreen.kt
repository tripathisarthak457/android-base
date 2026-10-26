package com.example.cmp.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cmp.core.datastore.settings.AppSettings
import com.example.cmp.core.designsystem.component.container.AppCard
import com.example.cmp.core.designsystem.component.container.AppDivider
import com.example.cmp.core.designsystem.component.container.AppScaffold
import com.example.cmp.core.designsystem.component.list.AppRow
import com.example.cmp.core.designsystem.component.list.AppSectionHeader
import com.example.cmp.core.designsystem.component.navigation.AppLargeTitleTopBar
import com.example.cmp.core.designsystem.component.overlay.AppAlert
import com.example.cmp.core.designsystem.component.selection.AppSegmentedControl
import com.example.cmp.core.designsystem.component.selection.AppSwitch
import com.example.cmp.core.designsystem.component.sheet.action.AppActionSheet
import com.example.cmp.core.designsystem.component.sheet.action.SheetAction
import com.example.cmp.core.designsystem.component.text.AppIcon
import com.example.cmp.core.designsystem.component.text.AppMonoText
import com.example.cmp.core.designsystem.component.text.AppText
import com.example.cmp.core.designsystem.foundation.layout.ReportScrollEdge
import com.example.cmp.core.designsystem.foundation.layout.chromeContentPadding
import com.example.cmp.core.designsystem.foundation.scroll.AppScrollDefaults
import com.example.cmp.core.designsystem.icon.AppIcons
import com.example.cmp.core.designsystem.theme.AppTheme
import com.example.cmp.core.ui.browser.rememberInAppBrowser
import com.example.cmp.core.ui.locale.AppLocales

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }
    val openInApp = rememberInAppBrowser()

    val scroll = rememberScrollState()
    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = {
            AppLargeTitleTopBar(
                title = stringResource(R.string.settings_title),
                scrolled = { scroll.value },
            )
        },
    ) {
        ReportScrollEdge(scroll)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll, flingBehavior = AppScrollDefaults.flingBehavior())
                .padding(chromeContentPadding(PaddingValues(horizontal = AppTheme.spacing.gutter))),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppSectionHeader(title = stringResource(R.string.settings_appearance))

            AppCard {
                AppText(
                    text = stringResource(R.string.settings_theme),
                    style = AppTheme.typography.subheadline,
                    color = AppTheme.colors.contentPrimary,
                )
                AppText(
                    text = stringResource(R.string.settings_theme_explanation),
                    modifier = Modifier.padding(bottom = AppTheme.spacing.md),
                    style = AppTheme.typography.caption1,
                    color = AppTheme.colors.contentTertiary,
                )
                AppSegmentedControl(
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    ),
                    selected = THEME_MODES.indexOf(state.settings.themeMode).coerceAtLeast(0),
                    onSelect = { onEvent(SettingsEvent.ThemeSelected(THEME_MODES[it])) },
                )
            }

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_haptic_feedback),
                    subtitle = stringResource(R.string.settings_haptics_explanation),
                    leading = {
                        AppIcon(
                            AppIcons.Bell,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                    trailing = {
                        AppSwitch(
                            checked = state.settings.hapticsEnabled,
                            onCheckedChange = { onEvent(SettingsEvent.HapticsToggled(it)) },
                        )
                    },
                )
            }

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_language),
                    subtitle = state.languageTag?.let(AppLocales::displayName)
                        ?: stringResource(R.string.settings_language_system),
                    onClick = { pickLanguage = true },
                    leading = {
                        AppIcon(
                            AppIcons.Globe,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                )
            }

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_require_unlock),
                    subtitle = stringResource(R.string.settings_app_lock_explanation),
                    leading = {
                        AppIcon(
                            AppIcons.Lock,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                    trailing = {
                        AppSwitch(
                            checked = state.settings.appLockEnabled,
                            onCheckedChange = { onEvent(SettingsEvent.AppLockToggled(it)) },
                        )
                    },
                )
            }

            NotificationsRow()

            AppSectionHeader(title = stringResource(R.string.settings_privacy))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_share_usage_data),
                    subtitle = stringResource(R.string.settings_analytics_explanation),
                    leading = {
                        AppIcon(
                            AppIcons.Info,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                    trailing = {
                        AppSwitch(
                            checked = state.settings.analyticsEnabled,
                            onCheckedChange = { onEvent(SettingsEvent.AnalyticsToggled(it)) },
                        )
                    },
                )
            }

            AppSectionHeader(title = stringResource(R.string.settings_account))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_sign_out),
                    onClick = { confirmSignOut = true },
                    leading = {
                        AppIcon(
                            AppIcons.Logout,
                            contentDescription = null,
                            tint = AppTheme.colors.danger.content,
                        )
                    },
                )
            }

            AppSectionHeader(title = stringResource(R.string.settings_about))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppRow(
                    title = stringResource(R.string.settings_open_source_licences),
                    subtitle = stringResource(R.string.settings_licences_explanation),
                    onClick = { onEvent(SettingsEvent.LicensesClicked) },
                    leading = {
                        AppIcon(
                            AppIcons.File,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                )
            }

            AppSectionHeader(title = stringResource(R.string.settings_legal))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                val privacyUrl = stringResource(R.string.settings_privacy_url)
                val termsUrl = stringResource(R.string.settings_terms_url)
                AppRow(
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { openInApp(privacyUrl) },
                    leading = {
                        AppIcon(
                            AppIcons.Lock,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                    trailing = {
                        AppIcon(
                            AppIcons.ExternalLink,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                )
                AppRow(
                    title = stringResource(R.string.settings_terms),
                    onClick = { openInApp(termsUrl) },
                    leading = {
                        AppIcon(
                            AppIcons.File,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                    trailing = {
                        AppIcon(
                            AppIcons.ExternalLink,
                            contentDescription = null,
                            tint = AppTheme.colors.contentTertiary,
                        )
                    },
                )
            }

            AppDivider(modifier = Modifier.padding(vertical = AppTheme.spacing.md))

            AppMonoText(
                text = stringResource(R.string.settings_version, state.appVersion),
                modifier = Modifier.padding(bottom = AppTheme.spacing.xxl),
                color = AppTheme.colors.contentTertiary,
            )
        }
    }

    val systemLabel = stringResource(R.string.settings_language_system)
    AppActionSheet(
        visible = pickLanguage,
        title = stringResource(R.string.settings_language),
        actions = listOf(
            SheetAction(
                label = systemLabel,
                onClick = { onEvent(SettingsEvent.LanguagePicked(null)) },
            ),
        ) +
            AppLocales.supported.map { tag ->
                SheetAction(
                    label = AppLocales.displayName(tag),
                    onClick = { onEvent(SettingsEvent.LanguagePicked(tag)) },
                )
            },
        onDismissRequest = { pickLanguage = false },
        cancelLabel = stringResource(R.string.settings_cancel),
    )

    AppAlert(
        visible = confirmSignOut,
        title = stringResource(R.string.settings_sign_out_confirm),
        message = stringResource(R.string.settings_sign_out_explanation),
        onDismissRequest = { confirmSignOut = false },
        confirmLabel = stringResource(R.string.settings_sign_out),
        onConfirm = { onEvent(SettingsEvent.SignOutConfirmed) },
        dismissLabel = stringResource(R.string.settings_stay),
        icon = AppIcons.Logout,
        destructive = true,
    )
}

/** The theme picker's segments, in order. */
private val THEME_MODES =
    listOf(AppSettings.THEME_SYSTEM, AppSettings.THEME_LIGHT, AppSettings.THEME_DARK)

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    AppTheme {
        SettingsScreen(
            state = SettingsState(appVersion = "1.0.0-devDebug"),
            onEvent = {},
        )
    }
}
