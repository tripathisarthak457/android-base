package io.example.audit.feature.sample.navigation

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet
import io.example.audit.core.navigation.AppNavigator
import io.example.audit.core.navigation.NavGraphEntry
import io.example.audit.core.navigation.NavPane
import io.example.audit.core.navigation.navGraph
import io.example.audit.core.navigation.navKeys
import io.example.audit.feature.sample.api.SampleDetailKey
import io.example.audit.feature.sample.api.SampleListKey
import io.example.audit.feature.sample.detail.SampleDetailRoute
import io.example.audit.feature.sample.list.SampleListRoute
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
}
