package com.example.none.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

/** The cases each rule must not flag, and the less obvious ones it must. */
class CodeRuleCasesTest {

    @Test
    fun `a lazy row with a key passes, and items outside a lazy scope are not rows`() {
        val source = """
            private fun List() = LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(rows, key = { it.id }, contentType = { "row" }) { Row(it) }
                items(count = 3, contentType = { "skeleton" }) { Skeleton() }
            }
            private fun load() {
                val all = items(store)
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(CORE_UI, source))
    }

    @Test
    fun `placeholder rows counted by a number have no identity to key on`() {
        val source = """
            private fun Loading() = LazyColumn {
                items(SKELETON_ROWS) { Skeleton() }
            }
            private fun Grid() = LazyRow {
                items(6) { Skeleton() }
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(CORE_UI, source))
    }

    @Test
    fun `a lazy scope extension holds its rows to the rule`() {
        val source = """
            fun LazyListScope.section(rows: List<Row>) {
                if (rows.isNotEmpty()) {
                    itemsIndexed(rows) { index, row -> Row(index, row) }
                }
            }
        """.trimIndent()

        assertEquals(listOf("3 lazy-key"), rules(CORE_UI, source))
    }

    @Test
    fun `a nested lazy row is its own scope for content types`() {
        val source = """
            private fun Feed() = LazyColumn {
                items(rows, key = { it.id }) { row ->
                    LazyRow { items(row.photos, key = { it.url }) { Photo(it) } }
                }
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(CORE_UI, source))
    }

    @Test
    fun `scrolling content padded by the chrome passes, and so does a row`() {
        val source = """
            private fun List() = LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = chromeContentPadding(PaddingValues(AppTheme.spacing.gutter)),
            ) {
                item(key = "photos") { LazyRow { items(photos, key = { it.url }) { Photo(it) } } }
            }
            private fun Detail() = Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag("detail")
                    .padding(chromeContentPadding(PaddingValues(AppTheme.spacing.gutter))),
            ) { }
            private fun Section(contentPadding: PaddingValues) = LazyVerticalGrid(
                columns = GridCells.Adaptive(AppTheme.layout.tileMinWidth),
                contentPadding = contentPadding,
            ) { }
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
    }

    @Test
    fun `padding around a scroll container clips its content at the bars`() {
        val source = """
            private fun Detail() = Column(
                Modifier
                    .padding(chromeContentPadding())
                    .verticalScroll(rememberScrollState()),
            ) { }
            private fun Grid() = LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(chromeContentPadding()),
            ) { }
            private fun Loading() = LazyColumn { }
        """.trimIndent()

        assertEquals(listOf("4 scroll-chrome", "6 scroll-chrome", "10 scroll-chrome"), rules(FEATURE, source))
    }

    @Test
    fun `copy in a preview is sample data, not shipped copy`() {
        val source = """
            @Preview
            @Composable
            private fun RowPreview() {
                AppText(text = "Hello there")
            }
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
    }

    @Test
    fun `runCatching is flagged only where it can swallow a cancellation`() {
        val source = """
            class Store(private val scope: CoroutineScope) {
                private val cached = runCatching { read() }
                init { runCatching { warm() } }
                fun parse(text: String) = runCatching { decode(text) }
                fun refresh() = scope.launch {
                    runCatching { fetch() }
                }
                suspend fun save(item: Item): Result<Unit> {
                    return runCatching { write(item) }
                }
                fun mapped() = items.map { runCatching { decode(it) } }
                suspend fun loaded() = withContext(io) { runCatching { fetch() } }
            }
        """.trimIndent()

        assertEquals(listOf("6 run-catching", "9 run-catching", "12 run-catching"), rules(DATA, source))
    }

    @Test
    fun `a class header just before a function does not make the function a class body`() {
        val source = """
            @Serializable
            data class OrderKey(val id: Int) : AppNavKey

            suspend fun load() {
                runCatching { fetch() }
            }
        """.trimIndent()

        assertEquals(listOf("5 run-catching"), rules(DATA, source))
    }

    @Test
    fun `comments that explain are not slop, and markers are not code`() {
        val source = """
            /** Fixed-width digits, so the timer does not jiggle as it counts. */
            /** Removed when the user signs out, with the rest of the session. */
            // When the list is empty the header still shows.
            // allow(raw-dp): the brand mark is drawn to the logo's own spec
            private val p = 16.dp
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
    }

    @Test
    fun `banners and todos are slop, in KDoc too`() {
        val source = """
            // ── Networking ──────────────────────────
            /** TODO: wire this to the real endpoint. */
            // ==========
        """.trimIndent()

        assertEquals(listOf("1 slop-comment", "2 slop-comment", "3 slop-comment"), rules(CORE_COMMON, source))
    }

    @Test
    fun `the project's own package is never judged as a layer`() {
        assertEquals(emptyList<String>(), rules(FEATURE, "package com.example.none.feature.orders.list"))
        assertEquals(emptyList<String>(), rules(DATA, "package com.example.none.data.orders.remote.dto"))
        assertEquals(listOf("1 layer-package"), rules(DATA, "package com.acme.app.data.orders.util"))
        assertEquals(
            listOf("1 layer-package"),
            rules("core/navigation/src/main/kotlin/A.kt", "package com.acme.app.core.navigation.common"),
        )
    }

    @Test
    fun `internal and private declarations, the api package and parameter lists pass feature-public`() {
        val source = """
            @Module
            @InstallIn(SingletonComponent::class)
            internal object OrdersNavigation

            @HiltViewModel
            internal class OrdersViewModel @Inject constructor(
                val repository: OrdersRepository,
            ) : MviViewModel<OrdersState, OrdersEvent, OrdersEffect>(OrdersState())

            private const val PAGE = 20
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
        assertEquals(
            emptyList<String>(),
            rules("feature/orders/src/main/kotlin/com/acme/app/feature/orders/api/OrdersKeys.kt", "data object OrdersKey"),
        )
    }

    @Test
    fun `a screen with its previews passes, a helper composable is not a screen`() {
        val source = """
            @Composable
            internal fun OrdersScreen() {}

            @Composable
            private fun OrderRow() {}

            @AppScreenPreviews
            @Composable
            private fun OrdersScreenPreview() {}
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
    }

    @Test
    fun `a screen takes a TextFieldState and a provider, but not a State`() {
        val source = """
            @Composable
            fun Search(
                query: TextFieldState,
                offset: () -> Float,
                progress: MutableState<Float>,
            ) {}
        """.trimIndent()

        assertEquals(listOf("5 state-param"), rules(DESIGN_SYSTEM, source))
    }

    @Test
    fun `the route takes the ViewModel`() {
        val source = """
            @Composable
            internal fun OrdersRoute(viewModel: OrdersViewModel = hiltViewModel()) {}
        """.trimIndent()

        assertEquals(emptyList<String>(), rules(FEATURE, source))
    }

    @Test
    fun `alpha on a small layer, or with ModulateAlpha, is not a full-screen fade`() {
        val source = """
            fun Modifier.dim(a: Float) = this.graphicsLayer { alpha = a }.size(24.dp)
            fun Modifier.fade(a: Float) = fillMaxSize().graphicsLayer {
                alpha = a
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            fun Modifier.veil(a: Float) = fillMaxSize().alpha(a)
        """.trimIndent()

        assertEquals(listOf("6 layer-alpha-fill"), rules(DESIGN_SYSTEM, source))
    }

    @Test
    fun `NavigationBackHandler is the replacement, not a legacy handler`() {
        assertEquals(emptyList<String>(), rules(CORE_UI, "fun f() { NavigationBackHandler(state) { } }"))
    }

    @Test
    fun `Material's adaptive types are allowed behind the scene and window-class seams only`() {
        val import = "import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy"

        assertEquals(emptyList<String>(), rules("core/navigation/src/main/kotlin/com/acme/app/core/navigation/scene/Panes.kt", import))
        assertEquals(listOf("1 adaptive-material-scope"), rules("core/navigation/src/main/kotlin/A.kt", import))
        assertEquals(
            listOf("1 adaptive-material-scope"),
            rules(
                "core/navigation/src/main/kotlin/com/acme/app/core/navigation/scene/Panes.kt",
                "import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold",
            ),
        )
    }

    @Test
    fun `the adaptive layer itself may read window metrics`() {
        val source = "val size = LocalWindowInfo.current.containerSize"

        assertEquals(emptyList<String>(), rules("core/designsystem/src/main/kotlin/a/foundation/adaptive/Window.kt", source))
        assertEquals(listOf("1 adaptive-screen-metrics"), rules(CORE_UI, source))
    }

    @Test
    fun `a commented line of prose is not code`() {
        val source = """
            // when a refresh fails, keep the list on screen
            // return(Nothing) is how the compiler sees it {}
        """.trimIndent()

        assertEquals(listOf("2 commented-code"), rules(CORE_COMMON, source))
    }

    private fun rules(path: String, source: String): List<String> =
        CodeRules.check(path, source).map { "${it.line} ${it.rule}" }

    private companion object {
        const val FEATURE = "feature/orders/src/main/kotlin/A.kt"
        const val DATA = "data/orders/src/main/kotlin/A.kt"
        const val CORE_UI = "core/ui/src/main/kotlin/A.kt"
        const val CORE_COMMON = "core/common/src/main/kotlin/A.kt"
        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin/A.kt"
    }
}
