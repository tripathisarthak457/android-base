package com.base.app.feature.settings

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
// <opt:licenses>
import com.base.app.feature.settings.api.LicensesKey
// </opt:licenses>
import com.base.app.feature.settings.api.SettingsAppInfo
import com.base.app.feature.settings.api.SettingsKey
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
