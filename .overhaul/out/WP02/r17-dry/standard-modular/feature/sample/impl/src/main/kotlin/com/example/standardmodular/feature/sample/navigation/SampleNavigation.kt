package com.example.standardmodular.feature.sample.navigation

import com.example.standardmodular.core.navigation.AppNavigator
import com.example.standardmodular.core.navigation.NavGraphEntry
import com.example.standardmodular.core.navigation.NavPane
import com.example.standardmodular.core.navigation.navGraph
import com.example.standardmodular.core.navigation.navKeys
import com.example.standardmodular.feature.sample.api.SampleDetailKey
import com.example.standardmodular.feature.sample.api.SampleListKey
import com.example.standardmodular.feature.sample.detail.SampleDetailRoute
import com.example.standardmodular.feature.sample.list.SampleListRoute
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
