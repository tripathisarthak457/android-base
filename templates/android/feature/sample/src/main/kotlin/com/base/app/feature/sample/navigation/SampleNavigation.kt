package com.base.app.feature.sample.navigation

import com.base.app.core.navigation.AppNavigator
// <opt:deeplink>
import com.base.app.core.navigation.DeepLinkRoute
// </opt:deeplink>
import com.base.app.core.navigation.NavGraphEntry
import com.base.app.core.navigation.NavPane
import com.base.app.core.navigation.navGraph
import com.base.app.core.navigation.navKeys
import com.base.app.feature.sample.api.SampleDetailKey
import com.base.app.feature.sample.api.SampleListKey
import com.base.app.feature.sample.detail.SampleDetailRoute
import com.base.app.feature.sample.list.SampleListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph, all `@IntoSet`: what to render for each key, how to
 * serialise the keys so the back stack survives process death, and which links open them.
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
    // <opt:deeplink>

    /** `baseapp://items` opens the list, `baseapp://items/12` the item. */
    @Provides
    @IntoSet
    fun sampleLinks(): DeepLinkRoute = DeepLinkRoute { link ->
        if (link.segments.first() != "items") return@DeepLinkRoute null
        link.segments.getOrNull(1)?.toIntOrNull()?.let(::SampleDetailKey) ?: SampleListKey
    }
    // </opt:deeplink>
}
