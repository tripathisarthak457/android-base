package com.example.standard.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.standard.core.common.session.SessionController
import com.example.standard.core.datastore.AppSettings
import com.example.standard.core.datastore.AppSettingsStore
import com.example.standard.core.designsystem.component.container.AppCard
import com.example.standard.core.designsystem.component.container.AppDivider
import com.example.standard.core.designsystem.component.container.AppListItem
import com.example.standard.core.designsystem.component.container.AppScaffold
import com.example.standard.core.designsystem.component.data.AppSectionHeader
import com.example.standard.core.designsystem.component.feedback.AppTone
import com.example.standard.core.designsystem.component.navigation.AppLargeTitle
import com.example.standard.core.designsystem.component.overlay.AppActionSheet
import com.example.standard.core.designsystem.component.overlay.AppAlertDialog
import com.example.standard.core.designsystem.component.overlay.SheetAction
import com.example.standard.core.designsystem.component.selection.AppSegmentedControl
import com.example.standard.core.designsystem.component.selection.AppSwitch
import com.example.standard.core.designsystem.component.text.AppIcon
import com.example.standard.core.designsystem.component.text.AppMonoText
import com.example.standard.core.designsystem.component.text.AppText
import com.example.standard.core.designsystem.icon.AppIcons
import com.example.standard.core.designsystem.theme.AppTheme
import com.example.standard.core.mvi.MviViewModel
import com.example.standard.core.mvi.UiEffect
import com.example.standard.core.mvi.UiEvent
import com.example.standard.core.mvi.UiState
import com.example.standard.core.navigation.AppNavigator
import com.example.standard.core.ui.MviScreen
import com.example.standard.core.ui.browser.rememberInAppBrowser
import com.example.standard.core.ui.locale.AppLocales
import com.example.standard.feature.settings.api.SettingsAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@Immutable
internal data class SettingsState(
    val appVersion: String,
    val settings: AppSettings = AppSettings(),
    /** Null while the app follows the phone's language. */
    val languageTag: String? = null,
) : UiState

internal sealed interface SettingsEvent : UiEvent {
    /** [mode] is one of the `AppSettings.THEME_*` values. */
    data class ThemeSelected(val mode: String) : SettingsEvent
    data class AnalyticsToggled(val enabled: Boolean) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent

    data object SignOutConfirmed : SettingsEvent

    /** The language lives in the platform rather than the store, so the route reads it. */
    data class LanguageRead(val tag: String?) : SettingsEvent
    data class LanguagePicked(val tag: String?) : SettingsEvent
}

internal sealed interface SettingsEffect : UiEffect {
    data class ApplyLanguage(val tag: String?) : SettingsEffect
}

/** Settings, reading and writing the store the rest of the app already uses. */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val sessionController: SessionController,
    appInfo: SettingsAppInfo,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState(appVersion = appInfo.versionName)) {

    init {
        observe(settingsStore.settings) { copy(settings = it) }
    }

    // Each write sets an absolute value, so a newer tap cancelling an older write loses nothing.
    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> launchLatest(THEME) { settingsStore.setThemeMode(event.mode) }

            is SettingsEvent.AnalyticsToggled -> launchLatest(ANALYTICS) { settingsStore.setAnalyticsEnabled(event.enabled) }

            is SettingsEvent.HapticsToggled -> launchLatest(HAPTICS) { settingsStore.setHapticsEnabled(event.enabled) }

            SettingsEvent.SignOutConfirmed -> sessionController.signOut()

            is SettingsEvent.LanguageRead -> setState { copy(languageTag = event.tag) }

            is SettingsEvent.LanguagePicked -> {
                setState { copy(languageTag = event.tag) }
                emit(SettingsEffect.ApplyLanguage(event.tag))
            }
        }
    }

    private companion object {
        const val THEME = "theme"
        const val ANALYTICS = "analytics"
        const val HAPTICS = "haptics"
    }
}

@Composable
internal fun SettingsRoute(
    navigator: AppNavigator,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.onEvent(SettingsEvent.LanguageRead(AppLocales.current(context))) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is SettingsEffect.ApplyLanguage -> AppLocales.set(context, effect.tag)
            }
        },
    ) { state, onEvent ->
        SettingsScreen(state = state, onEvent = onEvent)
    }
}

@Composable
internal fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    var pickLanguage by remember { mutableStateOf(false) }
    val openInApp = rememberInAppBrowser()

    AppScaffold(
        contentMaxWidth = AppTheme.layout.readableMaxWidth,
        modifier = modifier,
        topBar = { AppLargeTitle(title = stringResource(R.string.settings_title)) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppSectionHeader(title = stringResource(R.string.settings_appearance))

            AppCard {
                AppText(
                    text = stringResource(R.string.settings_theme),
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.contentPrimary,
                )
                AppText(
                    text = stringResource(R.string.settings_theme_explanation),
                    modifier = Modifier.padding(bottom = AppTheme.spacing.md),
                    style = AppTheme.typography.caption,
                    color = AppTheme.colors.contentTertiary,
                )
                AppSegmentedControl(
                    options = listOf(
                        stringResource(R.string.settings_theme_system),
                        stringResource(R.string.settings_theme_light),
                        stringResource(R.string.settings_theme_dark),
                    ),
                    selectedIndex = THEME_MODES.indexOf(state.settings.themeMode).coerceAtLeast(0),
                    onSelect = { onEvent(SettingsEvent.ThemeSelected(THEME_MODES[it])) },
                )
            }

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = stringResource(R.string.settings_haptic_feedback),
                    supporting = stringResource(R.string.settings_haptics_explanation),
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
                AppListItem(
                    title = stringResource(R.string.settings_language),
                    supporting = state.languageTag?.let(AppLocales::displayName)
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

            AppSectionHeader(title = stringResource(R.string.settings_privacy))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = stringResource(R.string.settings_share_usage_data),
                    supporting = stringResource(R.string.settings_analytics_explanation),
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
                AppListItem(
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

            AppSectionHeader(title = stringResource(R.string.settings_legal))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                val privacyUrl = stringResource(R.string.settings_privacy_url)
                val termsUrl = stringResource(R.string.settings_terms_url)
                AppListItem(
                    title = stringResource(R.string.settings_privacy_policy),
                    onClick = { openInApp(privacyUrl) },
                    leading = {
                        AppIcon(AppIcons.Lock, contentDescription = null, tint = AppTheme.colors.contentTertiary)
                    },
                    trailing = {
                        AppIcon(AppIcons.ExternalLink, contentDescription = null, tint = AppTheme.colors.contentTertiary)
                    },
                )
                AppListItem(
                    title = stringResource(R.string.settings_terms),
                    onClick = { openInApp(termsUrl) },
                    leading = {
                        AppIcon(AppIcons.File, contentDescription = null, tint = AppTheme.colors.contentTertiary)
                    },
                    trailing = {
                        AppIcon(AppIcons.ExternalLink, contentDescription = null, tint = AppTheme.colors.contentTertiary)
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

    if (pickLanguage) {
        val systemLabel = stringResource(R.string.settings_language_system)
        AppActionSheet(
            title = stringResource(R.string.settings_language),
            actions = listOf(
                SheetAction(label = systemLabel, onClick = { onEvent(SettingsEvent.LanguagePicked(null)) }),
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
    }

    if (confirmSignOut) {
        AppAlertDialog(
            title = stringResource(R.string.settings_sign_out_confirm),
            message = stringResource(R.string.settings_sign_out_explanation),
            onDismissRequest = { confirmSignOut = false },
            confirmLabel = stringResource(R.string.settings_sign_out),
            onConfirm = { onEvent(SettingsEvent.SignOutConfirmed) },
            dismissLabel = stringResource(R.string.settings_stay),
            icon = AppIcons.Logout,
            tone = AppTone.Error,
        )
    }
}

/** The theme picker's segments, in order. */
private val THEME_MODES = listOf(AppSettings.THEME_SYSTEM, AppSettings.THEME_LIGHT, AppSettings.THEME_DARK)

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
