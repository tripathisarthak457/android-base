package com.base.app.feature.settings

import com.base.app.core.navigation.AppNavKey
import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

/**
 * Settings is a destination like any other, whether it is a tab root or pushed from a profile row.
 */
@Serializable
data object SettingsKey : AppNavKey

// <opt:licenses>
@Serializable
data object LicensesKey : AppNavKey
// </opt:licenses>

/** The version string is supplied by whoever registers this graph. */
@Module
@InstallIn(SingletonComponent::class)
object SettingsNavModule {

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

/** What the settings screen needs to know about the build it is running in. */
data class SettingsAppInfo(val versionName: String)
