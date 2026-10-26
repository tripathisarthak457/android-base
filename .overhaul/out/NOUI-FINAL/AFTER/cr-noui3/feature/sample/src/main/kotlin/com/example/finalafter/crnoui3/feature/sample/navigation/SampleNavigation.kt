package com.example.finalafter.crnoui3.feature.sample.navigation

import com.example.finalafter.crnoui3.core.navigation.AppNavigator
import com.example.finalafter.crnoui3.core.navigation.DeepLinkRoute
import com.example.finalafter.crnoui3.core.navigation.NavGraphEntry
import com.example.finalafter.crnoui3.core.navigation.NavPane
import com.example.finalafter.crnoui3.core.navigation.navGraph
import com.example.finalafter.crnoui3.feature.sample.api.SampleDetailKey
import com.example.finalafter.crnoui3.feature.sample.api.SampleListKey
import com.example.finalafter.crnoui3.feature.sample.detail.SampleDetailRoute
import com.example.finalafter.crnoui3.feature.sample.list.SampleListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

/**
 * How this feature joins the navigation graph, all `@IntoSet`: what to render for each key, which
 * also lets the back stack save the key through process death, and which links open them.
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SampleNavigation {

    @Provides
    @IntoSet
    fun sampleNavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<SampleListKey>(pane = NavPane.List) { SampleListRoute(navigator = navigator) }
        entry<SampleDetailKey>(pane = NavPane.Detail) { key ->
            SampleDetailRoute(itemId = key.itemId, navigator = navigator)
        }
    }

    /** `finalafter://items` opens the list, `finalafter://items/12` the item. */
    @Provides
    @IntoSet
    fun sampleLinks(): DeepLinkRoute = DeepLinkRoute { link ->
        if (link.segments.first() != "items") return@DeepLinkRoute null
        link.segments.getOrNull(1)?.toIntOrNull()?.let(::SampleDetailKey) ?: SampleListKey
    }
}
