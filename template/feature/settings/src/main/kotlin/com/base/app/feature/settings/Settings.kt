package com.base.app.feature.settings

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
import com.base.app.core.common.mvi.MviViewModel
import com.base.app.core.common.mvi.UiEffect
import com.base.app.core.common.mvi.UiEvent
import com.base.app.core.common.mvi.UiState
import com.base.app.core.common.session.SessionController
import com.base.app.core.datastore.AppSettings
import com.base.app.core.datastore.AppSettingsStore
import com.base.app.core.designsystem.component.container.AppCard
import com.base.app.core.designsystem.component.container.AppDivider
import com.base.app.core.designsystem.component.container.AppListItem
import com.base.app.core.designsystem.component.container.AppScaffold
import com.base.app.core.designsystem.component.data.AppSectionHeader
import com.base.app.core.designsystem.component.feedback.AppTone
import com.base.app.core.designsystem.component.navigation.AppLargeTitle
import com.base.app.core.designsystem.component.overlay.AppAlertDialog
import com.base.app.core.designsystem.component.selection.AppSegmentedControl
import com.base.app.core.designsystem.component.selection.AppSwitch
import com.base.app.core.designsystem.component.text.AppIcon
import com.base.app.core.designsystem.component.text.AppMonoText
import com.base.app.core.designsystem.component.text.AppText
import com.base.app.core.designsystem.icon.AppIcons
import com.base.app.core.designsystem.theme.AppTheme
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.ui.MviScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
// <opt:language>
import androidx.compose.ui.platform.LocalContext
import com.base.app.core.designsystem.component.overlay.AppActionSheet
import com.base.app.core.designsystem.component.overlay.SheetAction
import com.base.app.core.ui.locale.AppLocales
// </opt:language>
// <opt:browser>
import com.base.app.core.ui.browser.rememberInAppBrowser
// </opt:browser>

@Immutable
data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val appVersion: String = "",
    // <opt:language>
    /** Null while the app follows the phone's language. */
    val languageTag: String? = null,
    // </opt:language>
) : UiState {

    val themeIndex: Int
        get() = when (settings.themeMode) {
            AppSettings.THEME_LIGHT -> 1
            AppSettings.THEME_DARK -> 2
            else -> 0
        }
}

sealed interface SettingsEvent : UiEvent {
    data class ThemeSelected(val index: Int) : SettingsEvent
    data class AnalyticsToggled(val enabled: Boolean) : SettingsEvent
    data class HapticsToggled(val enabled: Boolean) : SettingsEvent

    // <opt:applock>
    data class AppLockToggled(val enabled: Boolean) : SettingsEvent
    // </opt:applock>

    data object SignOutConfirmed : SettingsEvent

    // <opt:language>
    data class LanguagePicked(val tag: String?) : SettingsEvent
    // </opt:language>

    // <opt:licenses>
    data object LicensesClicked : SettingsEvent
    // </opt:licenses>

    data object BackClicked : SettingsEvent
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect

    // <opt:language>
    data class ApplyLanguage(val tag: String?) : SettingsEffect
    // </opt:language>

    // <opt:licenses>
    data object OpenLicenses : SettingsEffect
    // </opt:licenses>
}

/**
 * Settings, reading and writing the store the rest of the app already uses.
 *
 * ## The state is the store, not a copy of it
 *
 * Collecting `settingsStore.settings` into the state means the theme switch takes effect through
 * the same path as a change made anywhere else. Holding a local copy and writing to the store
 * separately gives two sources of truth for the same value, and they disagree the first time a
 * write fails.
 *
 * `WhileSubscribed(5_000)` keeps the collection alive briefly across a configuration change, so
 * rotating the screen does not tear down and re-establish it.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val sessionController: SessionController,
) : MviViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    init {
        launchWork {
            settingsStore.settings
                .stateIn(this, SharingStarted.WhileSubscribed(SUBSCRIBE_TIMEOUT_MILLIS), AppSettings())
                .collect { settings -> updateState { copy(settings = settings) } }
        }
    }

    override suspend fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeSelected -> settingsStore.setThemeMode(
                when (event.index) {
                    1 -> AppSettings.THEME_LIGHT
                    2 -> AppSettings.THEME_DARK
                    else -> AppSettings.THEME_SYSTEM
                },
            )

            is SettingsEvent.AnalyticsToggled -> settingsStore.setAnalyticsEnabled(event.enabled)

            is SettingsEvent.HapticsToggled -> settingsStore.setHapticsEnabled(event.enabled)

            // <opt:applock>
            is SettingsEvent.AppLockToggled -> settingsStore.setAppLockEnabled(event.enabled)
            // </opt:applock>

            SettingsEvent.SignOutConfirmed -> sessionController.signOut()

            // <opt:language>
            is SettingsEvent.LanguagePicked -> {
                updateState { copy(languageTag = event.tag) }
                emitEffect(SettingsEffect.ApplyLanguage(event.tag))
            }
            // </opt:language>

            // <opt:licenses>
            SettingsEvent.LicensesClicked -> emitEffect(SettingsEffect.OpenLicenses)
            // </opt:licenses>

            SettingsEvent.BackClicked -> emitEffect(SettingsEffect.NavigateBack)
        }
    }

    fun setAppVersion(version: String) {
        updateState { copy(appVersion = version) }
    }
    // <opt:language>

    /** The language lives in the platform rather than the store, so the route reads it. */
    fun setLanguage(tag: String?) {
        updateState { copy(languageTag = tag) }
    }
    // </opt:language>

    private companion object {
        const val SUBSCRIBE_TIMEOUT_MILLIS = 5_000L
    }
}

@Composable
fun SettingsRoute(
    navigator: AppNavigator,
    appVersion: String,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(appVersion) { viewModel.setAppVersion(appVersion) }
    // <opt:language>
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.setLanguage(AppLocales.current(context)) }
    // </opt:language>

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> navigator.navigateUp()
                // <opt:language>
                is SettingsEffect.ApplyLanguage -> AppLocales.set(context, effect.tag)
                // </opt:language>
                // <opt:licenses>
                SettingsEffect.OpenLicenses -> navigator.navigate(LicensesKey)
                // </opt:licenses>
            }
        },
    ) { state, onEvent ->
        SettingsScreen(state = state, onEvent = onEvent)
    }
}

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmSignOut by remember { mutableStateOf(false) }
    // <opt:language>
    var pickLanguage by remember { mutableStateOf(false) }
    // </opt:language>
    // <opt:browser>
    val openInApp = rememberInAppBrowser()
    // </opt:browser>

    AppScaffold(
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
                    options = listOf("System", "Light", "Dark"),
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

            // <opt:language>
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
            // </opt:language>

            // <opt:applock>
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
            // </opt:applock>

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

            // <opt:licenses>
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
            // </opt:licenses>

            // <opt:browser>
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
            // </opt:browser>

            AppDivider(modifier = Modifier.padding(vertical = AppTheme.spacing.md))

            AppMonoText(
                text = stringResource(R.string.settings_version, state.appVersion),
                modifier = Modifier.padding(bottom = AppTheme.spacing.xxl),
                color = AppTheme.colors.contentTertiary,
            )
        }
    }

    // <opt:language>
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
    // </opt:language>

    if (confirmSignOut) {
        AppAlertDialog(
            title = stringResource(R.string.settings_sign_out_confirm),
            message = stringResource(R.string.settings_sign_out_explanation),
            onDismissRequest = { confirmSignOut = false },
            confirmLabel = "Sign out",
            onConfirm = { onEvent(SettingsEvent.SignOutConfirmed) },
            dismissLabel = "Stay",
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
