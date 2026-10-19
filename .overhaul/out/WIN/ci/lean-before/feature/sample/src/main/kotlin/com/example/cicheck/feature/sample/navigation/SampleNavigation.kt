package com.example.cicheck.feature.sample.navigation

import com.example.cicheck.core.navigation.AppNavigator
import com.example.cicheck.core.navigation.NavGraphEntry
import com.example.cicheck.core.navigation.NavPane
import com.example.cicheck.core.navigation.navGraph
import com.example.cicheck.core.navigation.navKeys
import com.example.cicheck.feature.sample.api.SampleDetailKey
import com.example.cicheck.feature.sample.api.SampleListKey
import com.example.cicheck.feature.sample.detail.SampleDetailRoute
import com.example.cicheck.feature.sample.list.SampleListRoute
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
internal object SampleNavigation {

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
