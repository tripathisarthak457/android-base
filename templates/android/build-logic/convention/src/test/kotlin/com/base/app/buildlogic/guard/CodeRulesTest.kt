package com.base.app.buildlogic.guard

import com.base.app.buildlogic.guard.CodeRules.Finding
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
            private fun List() = LazyColumn(modifier = Modifier) {
                items(rows) { Row(it) } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "lazy-content-type",
            CORE_UI,
            """
            private fun List() = LazyColumn {
                item(contentType = "header") { Header() }
                items(rows, key = { it.id }) { Row(it) } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture(
            "scroll-chrome",
            FEATURE,
            """
            private fun List() = LazyColumn(modifier = Modifier.fillMaxSize()) { /*!*/
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
        val finding = CodeRules.check(CORE_UI, "private fun List() = LazyColumn {\n    items(rows) { Row(it) }\n}").single()

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
                LazyColumn { items(rows) { } } and 16.dp
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
        assertEquals(emptyList<Finding>(), CodeRules.check(DESIGN_SYSTEM, "val c = Color(0xFF000000)"))
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
    }
}
