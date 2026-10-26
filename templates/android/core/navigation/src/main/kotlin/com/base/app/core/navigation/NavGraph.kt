package com.base.app.core.navigation

import androidx.compose.runtime.Composable
import com.base.app.core.common.navigation.AppNavKey
import kotlin.reflect.KClass
import kotlinx.serialization.KSerializer
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer

/**
 * One feature's destinations. A feature builds one of these and contributes it to a Hilt
 * `@IntoSet`; the host merges every contribution into a single lookup.
 *
 * ```
 * @Module
 * @InstallIn(ActivityRetainedComponent::class)
 * internal object SampleNavigation {
 *     @Provides
 *     @IntoSet
 *     fun sampleNavGraph(): NavGraphEntry = navGraph {
 *         entry<SampleListKey>(pane = NavPane.List) { SampleListRoute() }
 *         entry<SampleDetailKey>(pane = NavPane.Detail) { key -> SampleDetailRoute(key.id) }
 *         entry<SampleFilterKey>(presentation = Presentation.Sheet()) { SampleFilterRoute() }
 *     }
 * }
 * ```
 *
 * Registering a key is also what lets the back stack save it: each `entry<K>` records K's
 * serializer, so a destination cannot be opened yet be missing when the process comes back.
 *
 * The keys of one graph belong together: a link to a detail opens the tab whose root key is
 * registered in the same graph.
 */
class NavGraphEntry internal constructor(
    internal val destinations: Map<KClass<out AppNavKey>, Destination>,
    /** How this graph's keys are written into a saved back stack and read back from it. */
    internal val keys: SerializersModule,
)

/** What a key renders and how: the registration a host reads through [NavRegistry.destinationFor]. */
@NavigationHostApi
class Destination internal constructor(
    val transition: NavTransitionStyle,
    val pane: NavPane,
    val presentation: Presentation?,
    val content: @Composable (AppNavKey) -> Unit,
)

class NavGraphBuilder internal constructor() {

    private val destinations = mutableMapOf<KClass<out AppNavKey>, Destination>()

    private val serializers = mutableListOf<PolymorphicModuleBuilder<AppNavKey>.() -> Unit>()

    /**
     * Registers [content] as the screen for key type [T], and [T]'s serializer for the saved back
     * stack, so [T] must be `@Serializable`. A [NavPane.List] and the [NavPane.Detail] opened from
     * it share the window when it is wide enough. A key with a [presentation] is shown above the
     * tab bar, as a sheet or a cover, wherever it is navigated to from.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T : AppNavKey> entry(
        transition: NavTransitionStyle = NavTransitionStyle.Push,
        pane: NavPane = NavPane.Single,
        presentation: Presentation? = null,
        noinline content: @Composable (T) -> Unit,
    ) {
        register(T::class, serializer<T>(), transition, pane, presentation) { key ->
            content(key as T)
        }
    }

    @PublishedApi
    internal fun <T : AppNavKey> register(
        type: KClass<T>,
        serializer: KSerializer<T>,
        transition: NavTransitionStyle,
        pane: NavPane,
        presentation: Presentation?,
        content: @Composable (AppNavKey) -> Unit,
    ) {
        require(
            destinations.put(type, Destination(transition, pane, presentation, content)) == null,
        ) {
            "${type.simpleName} is registered twice in the same nav graph."
        }
        serializers += { subclass(type, serializer) }
    }

    internal fun build(): NavGraphEntry = NavGraphEntry(
        destinations = destinations.toMap(),
        keys = SerializersModule {
            polymorphic(AppNavKey::class) { serializers.forEach { it(this) } }
        },
    )
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

    @NavigationHostApi
    fun destinationFor(key: AppNavKey): Destination = destinations[key::class] ?: error(
        "No destination registered for ${key::class.simpleName}. Add it to the feature's " +
            "navGraph { } block and contribute that with @Provides @IntoSet.",
    )

    @NavigationHostApi
    fun presentationOf(key: AppNavKey): Presentation? = destinations[key::class]?.presentation

    /** Whether [a] and [b] were registered by the same feature's graph. */
    @NavigationHostApi
    fun sameGraph(a: AppNavKey, b: AppNavKey): Boolean {
        val graph = graphOf[a::class]
        return graph != null && graph == graphOf[b::class]
    }

    /** Whether a destination for [type] is installed: optional features may be left out of a build. */
    fun has(type: KClass<out AppNavKey>): Boolean = type in destinations
}

/**
 * Which pane of a canonical layout a destination is. Panes opened over each other sit side by side
 * when the window has room, and stack as screens when it does not: the same back stack, the same
 * keys, nothing in the feature knowing which.
 */
enum class NavPane {
    /** A screen of its own. */
    Single,

    /** A list whose rows open a [Detail] beside it from medium widths on. */
    List,

    /** What a [List] row opens: beside the list when there is room, over it when not. */
    Detail,

    /** More about a [Detail] (its history, its attachments): a third column from large widths on. */
    Extra,

    /** A screen that a [Supporting] pane accompanies from expanded widths on: a map and its places. */
    Main,

    /** Beside its [Main] from expanded widths on, pushed over it below them. */
    Supporting,
}
