package com.example.finalafter.crui2.buildlogic.guard

import com.example.finalafter.crui2.buildlogic.guard.CodeRules.Finding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRulesTest {

    /** One source per rule. The line holding the `/*!*/` comment is the one the rule must flag. */
    private class Fixture(val rule: String, val path: String, val code: String, val composeCompiler: Boolean = true)

    private val fixtures = listOf(
        Fixture("ripple", DESIGN_SYSTEM, "import androidx.compose.material3.ripple /*!*/"),
        Fixture("material", DESIGN_SYSTEM, "import androidx.compose.material3.Text /*!*/"),
        Fixture("composed", DESIGN_SYSTEM, "fun Modifier.press() = composed { this } /*!*/"),
        Fixture("nav-fade", FEATURE, "private val m = NavDisplay.transitionSpec { null } /*!*/"),
        Fixture("collect-as-state", CORE_UI, "val s by flow.collectAsState() /*!*/"),
        Fixture("dispatchers-io", "core/network/src/main/kotlin/A.kt", "val d = Dispatchers.IO /*!*/"),
        Fixture("raw-tween", FEATURE, "private val s = tween<Float>(300) /*!*/"),
        Fixture("raw-color", CORE_UI, "val c = Color(0xFF000000) /*!*/"),
        Fixture("raw-dp", FEATURE, "private val p = 16.dp /*!*/"),
        Fixture(
            "subcompose-image",
            FEATURE,
            "private val i = SubcomposeAsyncImage(model = url, contentDescription = null) /*!*/",
        ),
        Fixture("bare-clickable", FEATURE, "private fun f(m: Modifier) = m.clickable { } /*!*/"),
        Fixture(
            "lazy-key",
            CORE_UI,
            """
            private fun List() = LazyColumn(flingBehavior = AppScrollDefaults.flingBehavior(), modifier = Modifier) {
                items(rows) { Row(it) } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "lazy-content-type",
            CORE_UI,
            """
            private fun List() = LazyColumn(flingBehavior = AppScrollDefaults.flingBehavior()) {
                item(contentType = "header") { Header() }
                items(rows, key = { it.id }) { Row(it) } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "scroll-chrome",
            FEATURE,
            """
            private fun List() = LazyColumn(flingBehavior = AppScrollDefaults.flingBehavior(), modifier = Modifier.fillMaxSize()) { /*!*/
                items(rows, key = { it.id }) { Row(it) }
            }
            """.trimIndent(),
        ),
        Fixture("state-param", DESIGN_SYSTEM, "@Composable fun Bar(offset: State<Float>) {} /*!*/"),
        Fixture("vm-forwarding", FEATURE, "@Composable internal fun OrdersBody(viewModel: OrdersViewModel) {} /*!*/"),
        Fixture(
            "animated-delegate",
            DESIGN_SYSTEM,
            """
            @Composable
            private fun Fade() {
                val x by animateFloatAsState(1f) /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "layer-alpha-fill",
            DESIGN_SYSTEM,
            """
            fun Modifier.scrim(a: Float) = this
                .fillMaxSize()
                .graphicsLayer { alpha = a } /*!*/
            """.trimIndent(),
        ),
        Fixture("pixel-line", DESIGN_SYSTEM, "fun DrawScope.f(c: Color) = drawLine(c, Offset.Zero, Offset(9f, 0f), strokeWidth = 1f) /*!*/"),
        Fixture("legacy-back", CORE_UI, "fun f() { BackHandler { } } /*!*/"),
        Fixture("experimental-api", DESIGN_SYSTEM, "@OptIn(ExperimentalGridApi::class) /*!*/"),
        Fixture("blocking", DATA, "val v = runBlocking { 1 } /*!*/"),
        Fixture("run-catching", DATA, "suspend fun load() = runCatching { api.get() } /*!*/"),
        Fixture("bang-bang", DATA, "val v = x!! /*!*/"),
        Fixture("slop-comment", CORE_COMMON, "// Added a retry here /*!*/"),
        Fixture("commented-code", CORE_COMMON, "// val retries = 3 /*!*/"),
        Fixture("file-length", CORE_COMMON, "package x /*!*/\n" + "\n".repeat(KotlinRules.MAX_FILE_LINES)),
        Fixture("spaced-test-name", "feature/orders/src/androidTest/kotlin/A.kt", "fun `opens the detail`() = Unit /*!*/"),
        Fixture("layer-package", FEATURE, "package com.acme.app.feature.orders.ui /*!*/"),
        Fixture("feature-public", FEATURE, "class OrderFormatter /*!*/"),
        Fixture(
            "uistate-mutable",
            "feature/orders/src/main/kotlin/OrdersContract.kt",
            "internal data class OrdersState(val rows: MutableList<Row>) /*!*/",
        ),
        Fixture(
            "api-immutable",
            "data/orders/src/main/kotlin/com/acme/app/data/orders/api/Order.kt",
            "data class Order(var id: Int) /*!*/",
        ),
        Fixture("hardcoded-copy", FEATURE, "private val t = AppText(text = \"Hello there\") /*!*/"),
        Fixture(
            "compose-compiler",
            FEATURE,
            "@Composable private fun A() {} /*!*/",
            composeCompiler = false,
        ),
        Fixture("adaptive-orientation", CORE_UI, "val land = configuration.orientation == ORIENTATION_LANDSCAPE /*!*/"),
        Fixture("adaptive-screen-metrics", CORE_UI, "val w = LocalConfiguration.current.screenWidthDp /*!*/"),
        Fixture("adaptive-device-type", CORE_UI, "val big = isTablet /*!*/"),
        Fixture("adaptive-fixed-size", CORE_UI, "val m = Modifier.width(400.dp) /*!*/"),
        Fixture(
            "adaptive-box-constraints",
            FEATURE,
            """
            @Composable
            private fun Body() {
                BoxWithConstraints { } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture("adaptive-mediaquery", CORE_UI, "val q = mediaQuery { } /*!*/"),
        Fixture(
            "adaptive-material-scope",
            FEATURE,
            "import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo /*!*/",
        ),
        Fixture(
            "adaptive-preview",
            FEATURE,
            """
            @Composable
            internal fun OrdersScreen() {} /*!*/
            """.trimIndent(),
        ),
        Fixture(
            "sheet-ime-padding",
            FEATURE,
            """
            @Composable
            private fun Rename() = AppSheet(visible = true, onDismissRequest = {}) {
                SheetColumn(Modifier.imePadding()) { } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture("sheet-shadow", SHEET, "private fun Modifier.card(elevation: Dp) = this.shadow(elevation) /*!*/"),
        Fixture("sheet-raw-delta", SHEET, "fun drag(s: AnchoredDraggableState<Int>) = s.dispatchRawDelta(1f) /*!*/"),
        Fixture("sheet-window", SHEET, "@Composable fun Card() { Popup(onDismissRequest = {}) { } } /*!*/"),
        Fixture(
            "sheet-conditional",
            FEATURE,
            """
            @Composable
            private fun Orders(show: Boolean) {
                if (show) {
                    AppSheet(visible = true, onDismissRequest = {}) { } /*!*/
                }
            }
            """.trimIndent(),
        ),
        Fixture(
            "sheet-modifier",
            FEATURE,
            """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}, modifier = Modifier.fillMaxWidth()) { } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "sheet-root",
            FEATURE,
            """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}) {
                    Header()
                    Body() /*!*/
                }
            }
            """.trimIndent(),
        ),
        Fixture("shell-seam", FEATURE, "@OptIn(ShellSeamApi::class) /*!*/"),
        Fixture(
            "sheet-when-else",
            FEATURE,
            """
            private fun label(d: SheetDetent) = when (d) { /*!*/
                SheetDetent.Large -> "large"
                SheetDetent.Medium -> "medium"
            }
            """.trimIndent(),
        ),
        Fixture("feature-primitive", FEATURE, "private fun f(m: Modifier) = m.border(border) /*!*/"),
        Fixture(
            "component-preview",
            DESIGN_SYSTEM,
            "package com.acme.core.designsystem.component /*!*/\n@Composable fun Tag(modifier: Modifier) = Box(modifier)",
        ),
        Fixture(
            "offscreen-layer",
            DESIGN_SYSTEM,
            "fun Modifier.frost() = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen } /*!*/",
        ),
        Fixture("designsystem-imports", DESIGN_SYSTEM, "import androidx.room.Dao /*!*/"),
        Fixture("stateless-params", DESIGN_SYSTEM, "@Composable fun Tag(rows: Flow<List<Row>>) {} /*!*/"),
        Fixture("raw-type", FEATURE, "private val s = 14.sp /*!*/"),
        Fixture("modifier-unused", DESIGN_SYSTEM, "@Composable private fun Tag(modifier: Modifier) {} /*!*/"),
        Fixture("composition-read", FEATURE, "private fun f(m: Modifier, fade: State<Float>) = m.alpha(fade.value) /*!*/"),
        Fixture("feature-composition-local", CORE_UI, "val LocalClock = staticCompositionLocalOf<Clock> { error(\"none\") } /*!*/"),
        Fixture("empty-description", FEATURE, "private fun f() = Icon(icon, contentDescription = \"\") /*!*/"),
        Fixture("navigation-host-api", FEATURE, "@OptIn(NavigationHostApi::class) /*!*/"),

        Fixture("scroll-fling", CORE_UI, "private fun Photos() = LazyRow(state = photos) { } /*!*/"),
    )

    @Test
    fun `every rule fires on its fixture, on the right line`() {
        val rules = CodeRules.ALL.map { it.id } + "compose-compiler"
        assertEquals(rules.toSet(), fixtures.map { it.rule }.toSet())

        fixtures.forEach { fixture ->
            val expectedLine = fixture.code.lines().indexOfFirst { "/*!*/" in it } + 1
            val findings = CodeRules.check(fixture.path, fixture.code, fixture.composeCompiler)
            assertEquals(fixture.rule, listOf(expectedLine to fixture.rule), findings.map { it.line to it.rule })
        }
    }

    @Test
    fun `an allow marker with a reason on the line above silences each rule`() {
        fixtures.forEach { fixture ->
            val findings = CodeRules.check(fixture.path, allowed(fixture.code, fixture.rule), fixture.composeCompiler)
            assertEquals(fixture.rule, emptyList<Finding>(), findings)
        }
    }

    @Test
    fun `rule ids are unique`() {
        val ids = CodeRules.ALL.map { it.id }

        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `a finding names the skill page that explains its fix`() {
        val finding = CodeRules.check(CORE_UI, "private fun List() = LazyColumn(flingBehavior = AppScrollDefaults.flingBehavior()) {\n    items(rows) { Row(it) }\n}").single()

        assertEquals("lazy-key", finding.rule)
        assertTrue(
            finding.message,
            finding.message.endsWith("(read .agents/skills/jetpack-compose-performance/references/lazy-layouts.md)"),
        )
    }

    @Test
    fun `an allow marker without a reason fails, and silences nothing`() {
        val source = "// allow(raw-dp)\nprivate val p = 16.dp"

        val findings = CodeRules.check(FEATURE, source)

        assertEquals(listOf(1 to "allow", 2 to "raw-dp"), findings.map { it.line to it.rule })
        assertTrue(findings.first().message.contains("needs a reason"))
    }

    @Test
    fun `a marker naming no rule, or silencing nothing, fails`() {
        val unknown = CodeRules.check(FEATURE, "// allow(raw-px): legacy\nprivate val p = 0.dp")
        val unused = CodeRules.check(FEATURE, "// allow(raw-dp): legacy\nprivate val p = 0.dp")

        assertEquals(listOf(1 to "allow"), unknown.map { it.line to it.rule })
        assertTrue(unknown.single().message.contains("raw-px"))
        assertEquals(listOf(1 to "allow"), unused.map { it.line to it.rule })
        assertTrue(unused.single().message.contains("delete the marker"))
    }

    @Test
    fun `one marker can name two rules on the same line`() {
        val source = "// allow(raw-dp, raw-color): the brand mark is drawn to the logo's own spec\n" +
            "private val m = Mark(16.dp, Color(0xFF112233))"

        assertEquals(emptyList<Finding>(), CodeRules.check(FEATURE, source))
    }

    @Test
    fun `patterns inside comments and strings are not code`() {
        val source = """
            // Dispatchers.IO and Modifier.clickable { } in a comment
            /* tween(300) /* nested */ runBlocking { } */
            private val a = "Modifier.clickable { } and Color(0xFF000000)"
            private val b = ${"\"\"\""}
                LazyColumn(flingBehavior = AppScrollDefaults.flingBehavior()) { items(rows) { } } and 16.dp
            ${"\"\"\""}
            private val c = '"'
            private fun `a test named runBlocking`() = Unit
        """.trimIndent()

        assertEquals(emptyList<Finding>(), CodeRules.check(FEATURE, source))
    }

    @Test
    fun `code inside a string template is code`() {
        val source = "val a = \"on ${'$'}{Dispatchers.IO.toString()}\""

        assertEquals(
            listOf(1 to "dispatchers-io"),
            CodeRules.check("core/network/src/main/kotlin/A.kt", source).map { it.line to it.rule },
        )
    }

    @Test
    fun `rules hold only in their scope`() {
        assertEquals(emptyList<Finding>(), CodeRules.check(CORE_UI, "val p = 16.dp"))
        // The tokens are where the hues are written; components read them.
        val theme = "core/designsystem/src/main/kotlin/com/acme/core/designsystem/theme/A.kt"
        assertEquals(emptyList<Finding>(), CodeRules.check(theme, "val c = Color(0xFF000000)"))
        assertEquals(emptyList<Finding>(), CodeRules.check("app/src/main/kotlin/com/acme/di/CoroutinesModule.kt", "val io = Dispatchers.IO"))
        assertEquals(emptyList<Finding>(), CodeRules.check("data/orders/src/test/kotlin/A.kt", "val v = runBlocking { 1 }"))
        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/test/kotlin/A.kt", "val c = Color(0xFF000000)"))
        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/test/kotlin/A.kt", "fun `loads the rows`() = Unit"))
    }

    @Test
    fun `a modular impl module is a feature module`() {
        val findings = CodeRules.check("feature/orders/impl/src/main/kotlin/A.kt", "private val p = 16.dp")

        assertEquals(listOf("raw-dp"), findings.map { it.rule })
    }

    @Test
    fun `a feature's build file cannot opt in to the navigation host's API either`() {
        val optIn = "kotlin {\n    compilerOptions {\n        optIn.add(\"com.acme.core.navigation.NavigationHostApi\")\n    }\n}"

        assertEquals(
            listOf(3 to "navigation-host-api"),
            CodeRules.check("feature/orders/impl/build.gradle.kts", optIn).map { it.line to it.rule },
        )
        assertEquals(emptyList<Finding>(), CodeRules.check("core/navigation/shell/build.gradle.kts", optIn))
        // Only the rules a build file can break read it.
        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/build.gradle.kts", "val p = 16.dp\nval io = Dispatchers.IO"))
    }

    private fun allowed(code: String, rule: String): String = code.lines().flatMap { line ->
        if ("/*!*/" in line) {
            listOf(line.takeWhile(Char::isWhitespace) + "// allow($rule): the fixture needs it", line)
        } else {
            listOf(line)
        }
    }.joinToString("\n")

    private companion object {
        const val FEATURE = "feature/orders/src/main/kotlin/A.kt"
        const val DATA = "data/orders/src/main/kotlin/A.kt"
        const val CORE_UI = "core/ui/src/main/kotlin/A.kt"
        const val CORE_COMMON = "core/common/src/main/kotlin/A.kt"
        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin/A.kt"
        const val SHEET = "core/designsystem/src/main/kotlin/com/acme/core/designsystem/component/sheet/A.kt"
    }
}
