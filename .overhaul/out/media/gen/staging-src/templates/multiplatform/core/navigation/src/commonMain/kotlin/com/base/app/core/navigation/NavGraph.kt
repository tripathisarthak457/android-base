package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.base.app.core.common.navigation.AppNavKey
import kotlin.reflect.KClass

/**
 * One feature's destinations. A feature builds one of these and contributes it to Koin under its
 * own qualifier; the app merges every contribution into a single lookup.
 *
 * ```
 * val sampleModule = module {
 *     single(named<SampleListKey>()) {
 *         navGraph {
 *             entry<SampleListKey>(pane = NavPane.List) { SampleListRoute(get()) }
 *             entry<SampleDetailKey>(pane = NavPane.Detail) { key -> SampleDetailRoute(key.itemId, get()) }
 *         }
 *     }
 * }
 * ```
 *
 * The keys of one graph belong together: a link to a detail opens the tab whose root key is
 * registered in the same graph.
 */
class NavGraphEntry internal constructor(
    internal val destinations: Map<KClass<out AppNavKey>, Destination>,
)

internal class Destination(
    val transition: NavTransitionStyle,
    val pane: NavPane,
    val presentation: Presentation?,
    val content: @Composable (AppNavKey) -> Unit,
)

class NavGraphBuilder internal constructor() {

    private val destinations = mutableMapOf<KClass<out AppNavKey>, Destination>()

    /**
     * Registers [content] as the screen for key type [T]. A [NavPane.List] and the [NavPane.Detail]
     * opened from it share the window when it is wide enough. A key with a [presentation] is shown
     * above the tab bar, as a sheet or a cover, wherever it is navigated to from.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : AppNavKey> entry(
        transition: NavTransitionStyle = NavTransitionStyle.Push,
        pane: NavPane = NavPane.Single,
        presentation: Presentation? = null,
        noinline content: @Composable (T) -> Unit,
    ) {
        register(T::class, transition, pane, presentation) { key -> content(key as T) }
    }

    @PublishedApi
    internal fun register(
        type: KClass<out AppNavKey>,
        transition: NavTransitionStyle,
        pane: NavPane,
        presentation: Presentation?,
        content: @Composable (AppNavKey) -> Unit,
    ) {
        require(destinations.put(type, Destination(transition, pane, presentation, content)) == null) {
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

    private val destinations = mutableMapOf<KClass<out AppNavKey>, Destination>()
    private val graphOf = mutableMapOf<KClass<out AppNavKey>, Int>()

    init {
        graphs.forEachIndexed { index, graph ->
            graph.destinations.forEach { (type, destination) ->
                require(destinations.put(type, destination) == null) {
                    "${type.simpleName} is registered by more than one feature's nav graph."
                }
                graphOf[type] = index
            }
        }
    }

    internal fun destinationFor(key: AppNavKey): Destination =
        destinations[key::class] ?: error(
            "No destination registered for ${key::class.simpleName}. Add it to the feature's " +
                "navGraph { } block and contribute that to Koin.",
        )

    internal fun presentationOf(key: AppNavKey): Presentation? = destinations[key::class]?.presentation

    /** Whether [a] and [b] were registered by the same feature's graph. */
    internal fun sameGraph(a: AppNavKey, b: AppNavKey): Boolean {
        val graph = graphOf[a::class]
        return graph != null && graph == graphOf[b::class]
    }

    /** Whether a destination for [type] is installed: optional features may be left out of a build. */
    fun has(type: KClass<out AppNavKey>): Boolean = type in destinations
}

/**
 * Which half of a list-detail pair a destination is. A [List] and the [Detail] opened from it sit
 * side by side when the window has room, and stack as two screens when it does not — the same
 * back stack, the same keys, nothing in the feature knowing which.
 */
enum class NavPane { Single, List, Detail }
