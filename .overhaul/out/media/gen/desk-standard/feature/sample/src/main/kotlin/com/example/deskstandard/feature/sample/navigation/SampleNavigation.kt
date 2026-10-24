package com.example.deskstandard.feature.sample.navigation

import com.example.deskstandard.core.navigation.AppNavigator
import com.example.deskstandard.core.navigation.DeepLinkRoute
import com.example.deskstandard.core.navigation.NavGraphEntry
import com.example.deskstandard.core.navigation.NavPane
import com.example.deskstandard.core.navigation.navGraph
import com.example.deskstandard.core.navigation.navKeys
import com.example.deskstandard.feature.sample.api.SampleDetailKey
import com.example.deskstandard.feature.sample.api.SampleListKey
import com.example.deskstandard.feature.sample.detail.SampleDetailRoute
import com.example.deskstandard.feature.sample.list.SampleListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph, all `@IntoSet`: what to render for each key, how to
 * serialise the keys so the back stack survives process death, and which links open them.
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

    @Provides
    @IntoSet
    fun sampleNavKeys(): SerializersModule = navKeys {
        subclass(SampleListKey::class, SampleListKey.serializer())
        subclass(SampleDetailKey::class, SampleDetailKey.serializer())
    }

    /** `deskstandard://items` opens the list, `deskstandard://items/12` the item. */
    @Provides
    @IntoSet
    fun sampleLinks(): DeepLinkRoute = DeepLinkRoute { link ->
        if (link.segments.first() != "items") return@DeepLinkRoute null
        link.segments.getOrNull(1)?.toIntOrNull()?.let(::SampleDetailKey) ?: SampleListKey
    }
}
