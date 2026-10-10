package com.base.app.feature.settings.navigation

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.settings.SettingsRoute
import com.base.app.feature.settings.SettingsViewModel
import com.base.app.feature.settings.api.SettingsKey
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The settings screen's ViewModel, its entry, and its key's serializer. Needs a `SettingsAppInfo`. */
val settingsModule = module {
    viewModelOf(::SettingsViewModel)
    single(named<SettingsKey>()) { settingsNavGraph() }
    single(named<SettingsKey>()) { settingsNavKeys() }
}

private fun settingsNavGraph(): NavGraphEntry = navGraph {
    entry<SettingsKey> { SettingsRoute() }
}

private fun settingsNavKeys(): SerializersModule = navKeys {
    subclass(SettingsKey::class, SettingsKey.serializer())
}
