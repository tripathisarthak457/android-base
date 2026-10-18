package com.example.hud.everything.feature.settings

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.hud.everything.core.common.session.SessionController
import com.example.hud.everything.core.datastore.settings.AppSettings
import com.example.hud.everything.core.datastore.settings.AppSettingsStore
import com.example.hud.everything.core.designsystem.component.container.AppCard
import com.example.hud.everything.core.designsystem.component.container.AppDivider
import com.example.hud.everything.core.designsystem.component.container.AppListItem
import com.example.hud.everything.core.designsystem.component.container.AppScaffold
import com.example.hud.everything.core.designsystem.component.data.AppSectionHeader
import com.example.hud.everything.core.designsystem.component.feedback.AppTone
import com.example.hud.everything.core.designsystem.component.navigation.AppLargeTitle
import com.example.hud.everything.core.designsystem.component.overlay.AppAlertDialog
import com.example.hud.everything.core.designsystem.component.selection.AppSegmentedControl
import com.example.hud.everything.core.designsystem.component.selection.AppSwitch
import com.example.hud.everything.core.designsystem.component.text.AppIcon
import com.example.hud.everything.core.designsystem.component.text.AppMonoText
import com.example.hud.everything.core.designsystem.component.text.AppText
import com.example.hud.everything.core.designsystem.icon.AppIcons
import com.example.hud.everything.core.designsystem.theme.AppTheme
import com.example.hud.everything.core.mvi.MviViewModel
import com.example.hud.everything.core.mvi.UiEffect
import com.example.hud.everything.core.mvi.UiEvent
import com.example.hud.everything.core.mvi.UiState
import com.example.hud.everything.core.navigation.AppNavigator
import com.example.hud.everything.core.ui.MviScreen
import com.example.hud.everything.feature.settings.api.LicensesKey
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.compose.ui.platform.LocalContext
import com.example.hud.everything.core.designsystem.component.overlay.AppActionSheet
import com.example.hud.everything.core.designsystem.component.overlay.SheetAction
import com.example.hud.everything.core.ui.locale.AppLocales
import com.example.hud.everything.core.ui.browser.rememberInAppBrowser

@Immutable
internal data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val appVersion: String = "",
    /** Null while the app follows the phone's language. */
    val languageTag: String? = null,
) : UiState {

    val themeIndex: Int
        get() = when (settings.themeMode) {
            AppSettings.THEME_LIGHT -> 1
            AppSettings.THEME_DARK -> 2
            else -> 0
        }
}

internal sealed interface SettingsEvent : UiEvent {
    data class ThemeSelected(val index: Int) : SettingsEvent
    data class AnalyticsToggled(val enabled: Boolean) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent

    data class AppLockToggled(val enabled: Boolean) : SettingsEvent

    data object SignOutConfirmed : SettingsEvent

    data class LanguagePicked(val tag: String?) : SettingsEvent

    data object LicensesClicked : SettingsEvent

    data object BackClicked : SettingsEvent
}

internal sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect

    data class ApplyLanguage(val tag: String?) : SettingsEffect

    data object OpenLicenses : SettingsEffect
}

/** Settings, reading and writing the store the rest of the app already uses. */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val sessionController: SessionController,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    init {
        observe(settingsStore.settings) { copy(settings = it) }
    }

    // Each write sets an absolute value, so a newer tap cancelling an older write loses nothing.
    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> launchLatest(THEME) {
                settingsStore.setThemeMode(
                    when (event.index) {
                        1 -> AppSettings.THEME_LIGHT
                        2 -> AppSettings.THEME_DARK
                        else -> AppSettings.THEME_SYSTEM
                    },
                )
            }

            is SettingsEvent.AnalyticsToggled -> launchLatest(ANALYTICS) { settingsStore.setAnalyticsEnabled(event.enabled) }

            is SettingsEvent.HapticsToggled -> launchLatest(HAPTICS) { settingsStore.setHapticsEnabled(event.enabled) }

            is SettingsEvent.AppLockToggled -> launchLatest(APP_LOCK) { settingsStore.setAppLockEnabled(event.enabled) }

            SettingsEvent.SignOutConfirmed -> sessionController.signOut()

            is SettingsEvent.LanguagePicked -> {
                setState { copy(languageTag = event.tag) }
                emit(SettingsEffect.ApplyLanguage(event.tag))
            }

            SettingsEvent.LicensesClicked -> emit(SettingsEffect.OpenLicenses)

            SettingsEvent.BackClicked -> emit(SettingsEffect.NavigateBack)
        }
    }

    fun setAppVersion(version: String) {
        setState { copy(appVersion = version) }
    }

    /** The language lives in the platform rather than the store, so the route reads it. */
    fun setLanguage(tag: String?) {
        setState { copy(languageTag = tag) }
    }

    private companion object {
        const val THEME = "theme"
        const val ANALYTICS = "analytics"
        const val HAPTICS = "haptics"
        const val APP_LOCK = "appLock"
    }
}

@Composable
internal fun SettingsRoute(
    navigator: AppNavigator,
    appVersion: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(appVersion) { viewModel.setAppVersion(appVersion) }
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.setLanguage(AppLocales.current(context)) }

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> navigator.navigateUp()
                is SettingsEffect.ApplyLanguage -> AppLocales.set(context, effect.tag)
                SettingsEffect.OpenLicenses -> navigator.navigate(LicensesKey)
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
                    selectedIndex = state.themeIndex,
                    onSelect = { onEvent(SettingsEvent.ThemeSelected(it)) },
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

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = stringResource(R.string.settings_require_unlock),
                    supporting = stringResource(R.string.settings_app_lock_explanation),
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

            AppSectionHeader(title = stringResource(R.string.settings_about))

            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = stringResource(R.string.settings_open_source_licences),
                    supporting = stringResource(R.string.settings_licences_explanation),
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
