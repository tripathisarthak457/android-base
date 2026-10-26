package com.example.finalafter.crnoui3.ui.start

import com.example.finalafter.crnoui3.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui3.core.navigation.navGraph
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

/** The start screen joins the navigation graph as a feature's screens do. */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object StartNavigation {

    @Provides
    @IntoSet
    fun startNavGraph(): NavGraphEntry = navGraph {
        entry<StartKey> { StartScreen() }
    }
}
