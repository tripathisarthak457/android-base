package com.example.finalafter.crnoui.feature.settings.navigation

import com.example.finalafter.crnoui.core.navigation.AppNavigator
import com.example.finalafter.crnoui.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui.core.navigation.navGraph
import com.example.finalafter.crnoui.feature.settings.SettingsRoute
import com.example.finalafter.crnoui.feature.settings.api.LicensesKey
import com.example.finalafter.crnoui.feature.settings.api.SettingsKey
import com.example.finalafter.crnoui.feature.settings.licenses.LicensesRoute
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
