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
 * How this feature joins the navigation graph. Two contributions, both `@IntoSet`: what to render
 * for each key, and how to serialise the keys so the back stack survives process death.
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
