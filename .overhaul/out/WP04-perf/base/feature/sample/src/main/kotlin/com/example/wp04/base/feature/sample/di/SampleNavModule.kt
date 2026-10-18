package com.example.wp04.base.feature.sample.di

import com.example.wp04.base.core.navigation.AppNavigator
import com.example.wp04.base.core.navigation.NavGraphEntry
import com.example.wp04.base.core.navigation.NavPane
import com.example.wp04.base.core.navigation.navGraph
import com.example.wp04.base.core.navigation.navKeys
import com.example.wp04.base.feature.sample.SampleDetailKey
import com.example.wp04.base.feature.sample.SampleDetailRoute
import com.example.wp04.base.feature.sample.SampleListKey
import com.example.wp04.base.feature.sample.SampleListRoute
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
        entry<SampleListKey>(pane = NavPane.List) { SampleListRoute(navigator = navigator) }
        entry<SampleDetailKey>(pane = NavPane.Detail) { key ->
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
