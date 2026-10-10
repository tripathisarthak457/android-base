package com.base.app.feature.onboarding.api

import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

/**
 * Where an app that has not finished onboarding starts. The app leaves onboarding by state, not by
 * navigating away: it shows the tabs once the completion flag is stored, so Back can never return
 * to a walkthrough the person has already finished.
 */
@Serializable
data object OnboardingKey : AppNavKey
