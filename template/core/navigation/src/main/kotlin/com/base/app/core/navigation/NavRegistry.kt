package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * One feature's destinations. A feature builds one of these and contributes it to a Hilt
 * `@IntoSet`; the host merges every contribution into a single lookup.
 *
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object SampleNavModule {
 *     @Provides
 *     @IntoSet
 *     fun sampleEntries(): NavGraphEntry = navGraph {
 *         entry<SampleListKey>(pane = NavPane.List) { SampleListRoute() }
 *         entry<SampleDetailKey>(pane = NavPane.Detail) { key -> SampleDetailRoute(key.id) }
 *     }
 * }
 * ```
 */
class NavGraphEntry internal constructor(
    internal val destinations: Map<KClass<out AppNavKey>, Destination>,
)

internal class Destination(
    val transition: NavTransitionStyle,
    val pane: NavPane,
    val content: @Composable (AppNavKey) -> Unit,
)

class NavGraphBuilder internal constructor() {

    private val destinations = mutableMapOf<KClass<out AppNavKey>, Destination>()

    /**
     * Registers [content] as the screen for key type [T]. A [NavPane.List] and the [NavPane.Detail]
     * opened from it share the window when it is wide enough.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : AppNavKey> entry(
        transition: NavTransitionStyle = NavTransitionStyle.Push,
        pane: NavPane = NavPane.Single,
        noinline content: @Composable (T) -> Unit,
    ) {
        register(T::class, transition, pane) { key -> content(key as T) }
    }

    @PublishedApi
    internal fun register(
        type: KClass<out AppNavKey>,
        transition: NavTransitionStyle,
        pane: NavPane,
        content: @Composable (AppNavKey) -> Unit,
    ) {
        require(destinations.put(type, Destination(transition, pane, content)) == null) {
            "${type.simpleName} is registered twice in the same nav graph."
        }
    }

    internal fun build(): NavGraphEntry = NavGraphEntry(destinations.toMap())
}

fun navGraph(builder: NavGraphBuilder.() -> Unit): NavGraphEntry =
    NavGraphBuilder().apply(builder).build()

/**
 * Every feature's destinations, merged. A duplicate registration fails at construction rather than
 * at the moment the second screen is opened.
 */
class NavRegistry(graphs: Set<NavGraphEntry>) {

    private val destinations: Map<KClass<out AppNavKey>, Destination> = buildMap {
        graphs.forEach { graph ->
            graph.destinations.forEach { (type, destination) ->
                val existing = put(type, destination)
                require(existing == null) {
                    "${type.simpleName} is registered by more than one feature's nav graph."
                }
            }
        }
    }

    internal fun destinationFor(key: AppNavKey): Destination =
        destinations[key::class] ?: error(
            "No destination registered for ${key::class.simpleName}. Add it to the feature's " +
                "navGraph { } block and contribute that with @Provides @IntoSet.",
        )

    fun transitionFor(key: AppNavKey): NavTransitionStyle = destinationFor(key).transition

    val registeredCount: Int get() = destinations.size
}
