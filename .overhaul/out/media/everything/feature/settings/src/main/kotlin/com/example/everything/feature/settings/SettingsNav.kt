package com.example.everything.feature.settings

import com.example.everything.core.navigation.AppNavigator
import com.example.everything.core.navigation.NavGraphEntry
import com.example.everything.core.navigation.navGraph
import com.example.everything.core.navigation.navKeys
import com.example.everything.feature.settings.api.LicensesKey
import com.example.everything.feature.settings.api.SettingsKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SettingsNavModule {

    @Provides
    @IntoSet
    fun settingsNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SettingsKey> {
            SettingsRoute(navigator = navigator)
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
