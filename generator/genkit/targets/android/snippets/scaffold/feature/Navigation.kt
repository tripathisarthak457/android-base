package __PKG__.feature.__NAME__.navigation

import __PKG__.core.navigation.AppNavigator
import __PKG__.core.navigation.NavGraphEntry
import __PKG__.core.navigation.NavPane
import __PKG__.core.navigation.navGraph
import __PKG__.core.navigation.navKeys
import __PKG__.feature.__NAME__.api.__P__DetailKey
import __PKG__.feature.__NAME__.api.__P__ListKey
import __PKG__.feature.__NAME__.detail.__P__DetailRoute
import __PKG__.feature.__NAME__.list.__P__ListRoute
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule

/**
 * How this feature joins the navigation graph: what renders each key, and how the keys are saved.
 * On a wide window the detail opens beside the list, because the two declare their panes.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object __P__Navigation {

    @Provides
    @IntoSet
    fun __CAMEL__NavGraph(navigator: AppNavigator): NavGraphEntry = navGraph {
        entry<__P__ListKey>(pane = NavPane.List) {
            __P__ListRoute(navigator = navigator)
        }
        entry<__P__DetailKey>(pane = NavPane.Detail) { key ->
            __P__DetailRoute(id = key.id, navigator = navigator)
        }
    }

    @Provides
    @IntoSet
    fun __CAMEL__NavKeys(): SerializersModule = navKeys {
__FIT__
        subclass(__P__ListKey::class, __P__ListKey.serializer())
__ELSE__
        subclass(
            __P__ListKey::class,
            __P__ListKey.serializer(),
        )
__END_FIT__
__FIT__
        subclass(__P__DetailKey::class, __P__DetailKey.serializer())
__ELSE__
        subclass(
            __P__DetailKey::class,
            __P__DetailKey.serializer(),
        )
__END_FIT__
    }
}
