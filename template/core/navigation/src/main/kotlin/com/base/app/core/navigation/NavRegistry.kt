package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import kotlin.reflect.KClass

/**
 * One feature's destinations.
 *
 * A feature builds one of these and contributes it to a Hilt `@IntoSet`; the host merges every
 * contribution into a single lookup. That is what makes adding a feature a change to *only* that
 * feature — no central `Route` sealed class to extend, no `when` in the app module to add a
 * branch to, and therefore no merge conflict on those two files every time two people add a
 * screen in the same week.
 *
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object SampleNavModule {
 *     @Provides
 *     @IntoSet
 *     fun sampleEntries(): NavGraphEntry = navGraph {
 *         entry<SampleListKey> { SampleListRoute() }
 *         entry<SampleDetailKey>(NavTransitionStyle.Push) { key -> SampleDetailRoute(key.id) }
 *     }
 * }
 * ```
 */
class NavGraphEntry internal constructor(
    internal val destinations: Map<KClass<out AppNavKey>, Destination>,
)

internal class Destination(
    val transition: NavTransitionStyle,
    val content: @Composable (AppNavKey) -> Unit,
)

class NavGraphBuilder internal constructor() {

    private val destinations = mutableMapOf<KClass<out AppNavKey>, Destination>()

    /**
     * Registers [content] as the screen for key type [T].
     *
     * The cast inside is safe by construction: the entry is stored under `T::class` and only ever
     * invoked with a key the registry looked up by that same class.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : AppNavKey> entry(
        transition: NavTransitionStyle = NavTransitionStyle.Push,
        noinline content: @Composable (T) -> Unit,
    ) {
        register(T::class, transition) { key -> content(key as T) }
    }

    @PublishedApi
    internal fun register(
        type: KClass<out AppNavKey>,
        transition: NavTransitionStyle,
        content: @Composable (AppNavKey) -> Unit,
    ) {
        require(destinations.put(type, Destination(transition, content)) == null) {
            "${type.simpleName} is registered twice in the same nav graph."
        }
    }

    internal fun build(): NavGraphEntry = NavGraphEntry(destinations.toMap())
}

fun navGraph(builder: NavGraphBuilder.() -> Unit): NavGraphEntry =
    NavGraphBuilder().apply(builder).build()

/**
 * Every feature's destinations, merged.
 *
 * A duplicate registration fails at construction rather than at the moment the second screen is
 * opened. Two features claiming the same key type is a real mistake — usually a copied module
 * whose keys were not renamed — and discovering it on a device three weeks later is much worse
 * than discovering it on the first launch after the build.
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
