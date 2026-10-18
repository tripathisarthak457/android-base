package com.example.wp04.abstd.feature.settings

import com.example.wp04.abstd.core.navigation.AppNavigator
import com.example.wp04.abstd.core.navigation.NavGraphEntry
import com.example.wp04.abstd.core.navigation.navGraph
import com.example.wp04.abstd.core.navigation.navKeys
import com.example.wp04.abstd.feature.settings.api.SettingsAppInfo
import com.example.wp04.abstd.feature.settings.api.SettingsKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/** The version string is supplied by whoever registers this graph. */
@Module
@InstallIn(ActivityRetainedComponent::class)
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
