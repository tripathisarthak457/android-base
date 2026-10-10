package com.base.app.feature.settings

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
// <opt:licenses>
import com.base.app.feature.settings.api.LicensesKey
// </opt:licenses>
import com.base.app.feature.settings.api.SettingsKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsNavModule {

    @Provides
    @IntoSet
    fun settingsNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SettingsKey> {
            SettingsRoute(navigator = navigator)
        }
        // <opt:licenses>
        entry<LicensesKey> {
            LicensesRoute(navigator = navigator)
        }
        // </opt:licenses>
    }

    @Provides
    @IntoSet
    fun settingsNavKeys(): SerializersModule = navKeys {
        subclass(SettingsKey::class, SettingsKey.serializer())
        // <opt:licenses>
        subclass(LicensesKey::class, LicensesKey.serializer())
        // </opt:licenses>
    }
}
