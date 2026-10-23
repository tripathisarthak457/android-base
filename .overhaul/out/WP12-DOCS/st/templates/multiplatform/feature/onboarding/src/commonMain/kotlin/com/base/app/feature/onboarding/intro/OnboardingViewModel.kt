package com.base.app.feature.onboarding.intro

import com.base.app.core.common.coroutines.ApplicationScope
import com.base.app.core.datastore.settings.AppSettingsStore
import com.base.app.core.mvi.MviViewModel
import kotlinx.coroutines.launch

/**
 * Onboarding, which is finished exactly once: "Get started" and "Skip" both store the completion
 * flag, and the app leaves onboarding when the flag lands. It has no effects to emit.
 */
internal class OnboardingViewModel(
    private val settingsStore: AppSettingsStore,
    private val appScope: ApplicationScope,
) : MviViewModel<OnboardingState, OnboardingEvent, Nothing>(OnboardingState()) {

    override fun handleEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Completed, OnboardingEvent.Skipped -> finish()
        }
    }

    // In the application scope: the stored flag replaces this screen, which clears this ViewModel
    // and would cancel a write running in its own scope.
    private fun finish() {
        appScope.launch { settingsStore.setOnboardingCompleted(true) }
    }
}
