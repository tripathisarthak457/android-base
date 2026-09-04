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

/**
 * The version string is supplied by whoever registers this graph.
 *
 * `BuildConfig` exists only in the application module, so a feature cannot read its own version —
 * and a feature that could would be reading whichever variant compiled *it*, not the app.
 */
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
    }

    @Provides
    @IntoSet
    fun settingsNavKeys(): SerializersModule = navKeys {
        subclass(SettingsKey::class, SettingsKey.serializer())
    }
}

/**
 * What the settings screen needs to know about the build it is running in.
 *
 * Provided by the application module. A data class rather than a bare `String` so it cannot
 * collide with another unqualified `String` binding in the graph — Hilt matches on type, and an
 * app with two unqualified `String` providers fails to compile with a message about neither.
 */
data class SettingsAppInfo(val versionName: String)
