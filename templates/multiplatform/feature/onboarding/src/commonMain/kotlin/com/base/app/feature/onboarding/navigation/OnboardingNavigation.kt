package com.base.app.feature.onboarding.navigation

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.onboarding.api.OnboardingKey
import com.base.app.feature.onboarding.intro.OnboardingRoute
import com.base.app.feature.onboarding.intro.OnboardingViewModel
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The onboarding screen's ViewModel, its entry, and its key's serializer. */
val onboardingModule = module {
    viewModelOf(::OnboardingViewModel)
    single(named<OnboardingKey>()) { onboardingNavGraph() }
    single(named<OnboardingKey>()) { onboardingNavKeys() }
}

private fun onboardingNavGraph(): NavGraphEntry = navGraph {
    entry<OnboardingKey> { OnboardingRoute() }
}

private fun onboardingNavKeys(): SerializersModule = navKeys {
    subclass(OnboardingKey::class, OnboardingKey.serializer())
}
