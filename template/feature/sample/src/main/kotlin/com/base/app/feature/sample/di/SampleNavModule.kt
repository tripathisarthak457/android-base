package com.base.app.feature.sample.di

import com.base.app.core.navigation.AppNavigator
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.sample.SampleDetailKey
import com.base.app.feature.sample.SampleDetailRoute
import com.base.app.feature.sample.SampleListKey
import com.base.app.feature.sample.SampleListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph.
 *
 * Two contributions, both `@IntoSet`: what to render for each key, and how to serialise the keys
 * so the back stack survives process death. Nothing outside this module is edited to add a
 * screen — no central sealed class, no `when` in the app module — which is what makes a feature
 * genuinely self-contained, and what stops two people adding screens in the same week from
 * conflicting on the same two files.
 *
 * Forgetting the [navKeys] half is the one mistake worth knowing about: the app works perfectly
 * until it is killed in the background, and then comes back at the start destination instead of
 * where the user was.
 */
@Module
@InstallIn(SingletonComponent::class)
object SampleNavModule {

    @Provides
    @IntoSet
    fun sampleNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SampleListKey> { SampleListRoute(navigator = navigator) }
        entry<SampleDetailKey> { key ->
            SampleDetailRoute(itemId = key.itemId, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun sampleNavKeys(): SerializersModule = navKeys {
        subclass(SampleListKey::class, SampleListKey.serializer())
        subclass(SampleDetailKey::class, SampleDetailKey.serializer())
    }
}
