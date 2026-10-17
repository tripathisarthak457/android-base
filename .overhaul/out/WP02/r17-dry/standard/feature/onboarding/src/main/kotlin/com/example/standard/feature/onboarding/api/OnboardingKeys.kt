package com.example.standard.feature.onboarding.api

import com.example.standard.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingKey : AppNavKey

/** Where the app goes once onboarding is finished. Supplied by `:app`, which knows the graph. */
data class OnboardingDestination(val next: AppNavKey)
