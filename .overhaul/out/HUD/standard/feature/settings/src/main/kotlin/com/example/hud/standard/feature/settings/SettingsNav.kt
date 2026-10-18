package com.example.hud.standard.feature.settings

import com.example.hud.standard.core.navigation.AppNavigator
import com.example.hud.standard.core.navigation.NavGraphEntry
import com.example.hud.standard.core.navigation.navGraph
import com.example.hud.standard.core.navigation.navKeys
import com.example.hud.standard.feature.settings.api.SettingsAppInfo
import com.example.hud.standard.feature.settings.api.SettingsKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/** The version string is supplied by whoever registers this graph. */
@Module
@InstallIn(SingletonComponent::class)
internal object SettingsNavModule {

    @Provides
    @IntoSet
    fun settingsNavGraph(
        navigator: AppNavigator,
        appInfo: SettingsAppInfo,
    ): NavGraphEntry = navGraph {
        entry<SettingsKey> {
            SettingsRoute(navigator = navigator, appVersion = appInfo.versionName)
        }
    }

    @Provides
    @IntoSet
    fun settingsNavKeys(): SerializersModule = navKeys {
        subclass(SettingsKey::class, SettingsKey.serializer())
    }
}
