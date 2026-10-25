package com.base.app.feature.settings.navigation

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
// <opt:licenses>
import com.base.app.feature.settings.LicensesRoute
// </opt:licenses>
import com.base.app.feature.settings.SettingsRoute
// <opt:licenses>
import com.base.app.feature.settings.api.LicensesKey
// </opt:licenses>
import com.base.app.feature.settings.api.SettingsKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SettingsNavigation {

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
}
