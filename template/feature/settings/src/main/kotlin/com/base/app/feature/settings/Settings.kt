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

@Immutable
data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val appVersion: String = "",
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

    // <opt:licenses>
    data object LicensesClicked : SettingsEvent
    // </opt:licenses>

    data object BackClicked : SettingsEvent
}

sealed interface SettingsEffect : UiEffect {
    data object NavigateBack : SettingsEffect

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

            // <opt:licenses>
            SettingsEvent.LicensesClicked -> emitEffect(SettingsEffect.OpenLicenses)
            // </opt:licenses>

            SettingsEvent.BackClicked -> emitEffect(SettingsEffect.NavigateBack)
        }
    }

    fun setAppVersion(version: String) {
        updateState { copy(appVersion = version) }
    }

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

    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                SettingsEffect.NavigateBack -> navigator.navigateUp()
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

    AppScaffold(
        modifier = modifier,
        topBar = { AppLargeTitle(title = stringResource(R.string.settings_settings)) },
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
                    text = stringResource(R.string.settings_system_follows_your_device_setting),
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
                    supporting = stringResource(R.string.settings_a_small_vibration_when_a_control),
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

            // <opt:applock>
            AppCard(contentPadding = PaddingValues(0.dp)) {
                AppListItem(
                    title = stringResource(R.string.settings_require_unlock),
                    supporting = stringResource(R.string.settings_ask_for_your_fingerprint_face_or) +
                        "been in the background.",
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
                    supporting = stringResource(R.string.settings_helps_us_find_crashes_and_slow),
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
                    supporting = stringResource(R.string.settings_the_libraries_this_app_is_built),
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

            AppDivider(modifier = Modifier.padding(vertical = AppTheme.spacing.md))

            AppMonoText(
                text = stringResource(R.string.settings_version, state.appVersion),
                modifier = Modifier.padding(bottom = AppTheme.spacing.xxl),
                color = AppTheme.colors.contentTertiary,
            )
        }
    }

    if (confirmSignOut) {
        AppAlertDialog(
            title = stringResource(R.string.settings_sign_out_2),
            message = stringResource(R.string.settings_you_will_need_to_sign_in),
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
