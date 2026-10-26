package com.example.finalafter.crui2.feature.settings.navigation

import com.example.finalafter.crui2.core.navigation.AppNavigator
import com.example.finalafter.crui2.core.navigation.NavGraphEntry
import com.example.finalafter.crui2.core.navigation.navGraph
import com.example.finalafter.crui2.feature.settings.SettingsRoute
import com.example.finalafter.crui2.feature.settings.api.LicensesKey
import com.example.finalafter.crui2.feature.settings.api.SettingsKey
import com.example.finalafter.crui2.feature.settings.licenses.LicensesRoute
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
        entry<LicensesKey> {
            LicensesRoute(navigator = navigator)
        }
    }
}
