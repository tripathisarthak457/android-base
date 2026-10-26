package com.example.finalafter.crui2.feature.settings

import app.cash.turbine.test
import com.example.finalafter.crui2.core.common.coroutines.ApplicationScope
import com.example.finalafter.crui2.core.datastore.FakePreferencesDataStore
import com.example.finalafter.crui2.core.datastore.settings.AppSettings
import com.example.finalafter.crui2.core.datastore.settings.AppSettingsStore
import com.example.finalafter.crui2.core.testing.FakeSessionController
import com.example.finalafter.crui2.core.testing.MainDispatcherRule
import com.example.finalafter.crui2.feature.settings.api.SettingsAppInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val session = FakeSessionController()

    @Test
    fun `the screen shows the build's version and the stored settings`() = runTest {
        val store = store()
        store.setThemeMode(AppSettings.THEME_DARK)
        val viewModel = watched(store)

        assertEquals("1.2.3", viewModel.state.value.appVersion)
        assertEquals(AppSettings.THEME_DARK, viewModel.state.value.settings.themeMode)
    }

    @Test
    fun `a change made elsewhere reaches the screen`() = runTest {
        val store = store()
        val viewModel = watched(store)

        store.setHapticsEnabled(false)
        runCurrent()

        assertEquals(false, viewModel.state.value.settings.hapticsEnabled)
    }

    @Test
    fun `picking a theme stores it`() = runTest {
        val store = store()
        val viewModel = watched(store)

        viewModel.onEvent(SettingsEvent.ThemeSelected(AppSettings.THEME_LIGHT))
        runCurrent()

        assertEquals(AppSettings.THEME_LIGHT, store.settings.first().themeMode)
        assertEquals(AppSettings.THEME_LIGHT, viewModel.state.value.settings.themeMode)
    }

    @Test
    fun `the switches store what they were set to`() = runTest {
        val store = store()
        val viewModel = watched(store)

        viewModel.onEvent(SettingsEvent.AnalyticsToggled(false))
        viewModel.onEvent(SettingsEvent.HapticsToggled(false))
        runCurrent()

        assertEquals(false, store.settings.first().analyticsEnabled)
        assertEquals(false, store.settings.first().hapticsEnabled)
    }

    @Test
    fun `of quick picks the last one is what stays`() = runTest {
        val store = store()
        val viewModel = watched(store)

        viewModel.onEvent(SettingsEvent.ThemeSelected(AppSettings.THEME_DARK))
        viewModel.onEvent(SettingsEvent.ThemeSelected(AppSettings.THEME_LIGHT))
        runCurrent()

        assertEquals(AppSettings.THEME_LIGHT, store.settings.first().themeMode)
    }

    @Test
    fun `the app lock switch stores what it was set to`() = runTest {
        val store = store()
        val viewModel = watched(store)

        viewModel.onEvent(SettingsEvent.AppLockToggled(true))
        runCurrent()

        assertEquals(true, store.settings.first().appLockEnabled)
    }

    @Test
    fun `a confirmed sign-out ends the session once`() = runTest {
        val viewModel = watched(store())

        viewModel.onEvent(SettingsEvent.SignOutConfirmed)

        assertEquals(1, session.signOuts)
    }

    @Test
    fun `the language the platform reports is shown`() = runTest {
        val viewModel = watched(store())

        viewModel.onEvent(SettingsEvent.LanguageRead("es"))

        assertEquals("es", viewModel.state.value.languageTag)
    }

    @Test
    fun `a picked language is shown and handed to the platform to apply`() = runTest {
        val viewModel = watched(store())

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.LanguagePicked("hi"))
            assertEquals(SettingsEffect.ApplyLanguage("hi"), awaitItem())
        }
        assertEquals("hi", viewModel.state.value.languageTag)
    }

    @Test
    fun `going back to the phone's language is a pick of no language`() = runTest {
        val viewModel = watched(store())
        viewModel.onEvent(SettingsEvent.LanguageRead("es"))

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.LanguagePicked(null))
            assertEquals(SettingsEffect.ApplyLanguage(null), awaitItem())
        }
        assertEquals(null, viewModel.state.value.languageTag)
    }

    @Test
    fun `the licences row opens the licences`() = runTest {
        val viewModel = watched(store())

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.LicensesClicked)
            assertEquals(SettingsEffect.OpenLicenses, awaitItem())
        }
    }

    /**
     * The store keeps the settings in a flow it shares in the background scope, so each step below
     * ends with runCurrent: advanceUntilIdle returns while that flow still has a value to pass on.
     */
    private fun TestScope.store() =
        AppSettingsStore(FakePreferencesDataStore(), ApplicationScope(backgroundScope))

    /** A settings screen on show, so the store's flow reaches its state. */
    private fun TestScope.watched(store: AppSettingsStore): SettingsViewModel =
        SettingsViewModel(store, session, SettingsAppInfo(versionName = "1.2.3")).also {
            it.state.launchIn(backgroundScope)
            runCurrent()
        }
}
