package com.example.hud.fixture.feature.onboarding.api

import com.example.hud.fixture.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingKey : AppNavKey

/** Where the app goes once onboarding is finished. Supplied by `:app`, which knows the graph. */
data class OnboardingDestination(val next: AppNavKey)
