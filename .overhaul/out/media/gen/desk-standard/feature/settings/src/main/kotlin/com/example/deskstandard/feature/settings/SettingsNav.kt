package com.example.deskstandard.feature.settings

import com.example.deskstandard.core.navigation.AppNavigator
import com.example.deskstandard.core.navigation.NavGraphEntry
import com.example.deskstandard.core.navigation.navGraph
import com.example.deskstandard.core.navigation.navKeys
import com.example.deskstandard.feature.settings.api.SettingsKey
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
    }

    @Provides
    @IntoSet
    fun settingsNavKeys(): SerializersModule = navKeys {
        subclass(SettingsKey::class, SettingsKey.serializer())
    }
}
