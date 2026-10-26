// Every feature's keys are in its api package's <Feature>Keys.kt, one key or several.
@file:Suppress("ktlint:standard:filename")

package com.base.app.feature.onboarding.api

import com.base.app.core.common.navigation.AppNavKey
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingKey : AppNavKey
