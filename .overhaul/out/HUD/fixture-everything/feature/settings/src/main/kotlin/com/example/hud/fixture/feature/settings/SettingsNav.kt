package com.example.hud.fixture.feature.settings

import com.example.hud.fixture.core.navigation.AppNavigator
import com.example.hud.fixture.core.navigation.NavGraphEntry
import com.example.hud.fixture.core.navigation.navGraph
import com.example.hud.fixture.core.navigation.navKeys
import com.example.hud.fixture.feature.settings.api.LicensesKey
import com.example.hud.fixture.feature.settings.api.SettingsAppInfo
import com.example.hud.fixture.feature.settings.api.SettingsKey
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
        entry<LicensesKey> {
            LicensesRoute(navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun settingsNavKeys(): SerializersModule = navKeys {
        subclass(SettingsKey::class, SettingsKey.serializer())
        subclass(LicensesKey::class, LicensesKey.serializer())
    }
}
