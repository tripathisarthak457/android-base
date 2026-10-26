package com.example.finalafter.crnoui3.feature.onboarding

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.cash.turbine.test
import com.example.finalafter.crnoui3.core.common.coroutines.ApplicationScope
import com.example.finalafter.crnoui3.core.datastore.FakePreferencesDataStore
import com.example.finalafter.crnoui3.core.datastore.settings.AppSettingsStore
import com.example.finalafter.crnoui3.core.testing.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `finishing leaves onboarding and records that it is done`() = runTest {
        val settings = settings()
        val viewModel = OnboardingViewModel(settings, ApplicationScope(backgroundScope))

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.Completed)
            assertEquals(OnboardingEffect.Finished, awaitItem())
        }
        runCurrent()

        assertTrue(settings.settings.first().onboardingCompleted)
    }

    @Test
    fun `skipping counts as finishing, so onboarding never comes back`() = runTest {
        val settings = settings()
        val viewModel = OnboardingViewModel(settings, ApplicationScope(backgroundScope))

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.Skipped)
            assertEquals(OnboardingEffect.Finished, awaitItem())
        }
        runCurrent()

        assertTrue(settings.settings.first().onboardingCompleted)
    }

    @Test
    fun `the flag is written even though leaving clears the screen at once`() = runTest {
        val settings = settings()
        val screens = ViewModelStore()
        val factory = viewModelFactory {
            initializer { OnboardingViewModel(settings, ApplicationScope(backgroundScope)) }
        }
        val viewModel = ViewModelProvider.create(screens, factory)[OnboardingViewModel::class]

        viewModel.onEvent(OnboardingEvent.Completed)
        screens.clear()
        runCurrent()

        assertTrue(settings.settings.first().onboardingCompleted)
    }

    /**
     * Settings in memory, shared in the background scope; each test ends with runCurrent because
     * advanceUntilIdle returns while only background work is queued.
     */
    private fun TestScope.settings() =
        AppSettingsStore(FakePreferencesDataStore(), ApplicationScope(backgroundScope))
}
