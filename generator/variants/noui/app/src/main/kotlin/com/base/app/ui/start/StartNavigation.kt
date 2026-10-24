package com.base.app.ui.start

import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/** The start screen joins the navigation graph as a feature's screens do. */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object StartNavigation {

    @Provides
    @IntoSet
    fun startNavGraph(): NavGraphEntry = navGraph {
        entry<StartKey> { StartScreen() }
    }

    @Provides
    @IntoSet
    fun startNavKeys(): SerializersModule = navKeys {
        subclass(StartKey::class, StartKey.serializer())
    }
}
