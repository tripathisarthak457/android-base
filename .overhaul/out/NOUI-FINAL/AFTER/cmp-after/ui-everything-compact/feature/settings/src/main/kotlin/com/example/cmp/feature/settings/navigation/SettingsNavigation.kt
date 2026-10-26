package com.example.cmp.feature.settings.navigation

import com.example.cmp.core.navigation.AppNavigator
import com.example.cmp.core.navigation.NavGraphEntry
import com.example.cmp.core.navigation.navGraph
import com.example.cmp.feature.settings.SettingsRoute
import com.example.cmp.feature.settings.api.LicensesKey
import com.example.cmp.feature.settings.api.SettingsKey
import com.example.cmp.feature.settings.licenses.LicensesRoute
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
