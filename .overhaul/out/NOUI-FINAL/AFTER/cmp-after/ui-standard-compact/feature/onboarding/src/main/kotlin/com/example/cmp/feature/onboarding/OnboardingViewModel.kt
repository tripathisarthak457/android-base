package com.example.cmp.feature.onboarding

import com.example.cmp.core.common.coroutines.ApplicationScope
import com.example.cmp.core.datastore.settings.AppSettingsStore
import com.example.cmp.core.mvi.MviViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Onboarding, which is finished exactly once. Both "Get started" and "Skip" write the same flag and
 * emit the same effect.
 */
@HiltViewModel
internal class OnboardingViewModel @Inject constructor(
    private val settingsStore: AppSettingsStore,
    private val appScope: ApplicationScope,
) : MviViewModel<OnboardingState, OnboardingEvent, OnboardingEffect>(OnboardingState()) {

    override fun handleEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Completed, OnboardingEvent.Skipped -> {
                emit(OnboardingEffect.Finished)
                // The navigation does not wait for the disk, and the write runs in the application
                // scope because leaving this screen clears this ViewModel, and its scope with it.
                appScope.launch { settingsStore.setOnboardingCompleted(true) }
            }
        }
    }
}
