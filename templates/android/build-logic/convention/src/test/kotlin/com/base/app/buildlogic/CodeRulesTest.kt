package com.base.app.buildlogic

import com.base.app.buildlogic.CodeRules.Finding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRulesTest {

    /** One source per rule. The line holding the `/*!*/` comment is the one the rule must flag. */
    private class Fixture(val rule: String, val path: String, val code: String, val composeCompiler: Boolean = true)

    private val fixtures = listOf(
        Fixture("ripple", "core/designsystem/src/main/kotlin/A.kt", "import androidx.compose.material3.ripple /*!*/"),
        Fixture("material", "core/designsystem/src/main/kotlin/A.kt", "import androidx.compose.material3.Text /*!*/"),
        Fixture("composed", "core/designsystem/src/main/kotlin/A.kt", "fun Modifier.press() = composed { this } /*!*/"),
        Fixture("nav-fade", "feature/orders/src/main/kotlin/A.kt", "val m = NavDisplay.transitionSpec { null } /*!*/"),
        Fixture("collect-as-state", "core/ui/src/main/kotlin/A.kt", "val s by flow.collectAsState() /*!*/"),
        Fixture("dispatchers-io", "core/network/src/main/kotlin/A.kt", "val d = Dispatchers.IO /*!*/"),
        Fixture("raw-tween", "feature/orders/src/main/kotlin/A.kt", "val s = tween<Float>(300) /*!*/"),
        Fixture("raw-color", "core/ui/src/main/kotlin/A.kt", "val c = Color(0xFF000000) /*!*/"),
        Fixture("raw-dp", "feature/orders/src/main/kotlin/A.kt", "val p = 16.dp /*!*/"),
        Fixture(
            "subcompose-image",
            "feature/orders/src/main/kotlin/A.kt",
            "val i = SubcomposeAsyncImage(model = url, contentDescription = null) /*!*/",
        ),
        Fixture("bare-clickable", "feature/orders/src/main/kotlin/A.kt", "fun f(m: Modifier) = m.clickable { } /*!*/"),
        Fixture(
            "lazy-key",
            "feature/orders/src/main/kotlin/A.kt",
            """
            fun List() = LazyColumn(modifier = Modifier) {
                items(rows) { Row(it) } /*!*/
            }
            """.trimIndent(),
        ),
        Fixture("blocking", "data/orders/src/main/kotlin/A.kt", "val v = runBlocking { 1 } /*!*/"),
        Fixture("hardcoded-copy", "feature/orders/src/main/kotlin/A.kt", "val t = AppText(text = \"Hello there\") /*!*/"),
        Fixture(
            "compose-compiler",
            "feature/orders/src/main/kotlin/A.kt",
            "@Composable fun A() {} /*!*/",
            composeCompiler = false,
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
    fun `an allow marker without a reason fails, and silences nothing`() {
        val source = "// allow(raw-dp)\nval p = 16.dp"

        val findings = CodeRules.check("feature/orders/src/main/kotlin/A.kt", source)

        assertEquals(listOf(1 to "allow", 2 to "raw-dp"), findings.map { it.line to it.rule })
        assertTrue(findings.first().message.contains("needs a reason"))
    }

    @Test
    fun `a marker naming no rule, or silencing nothing, fails`() {
        val unknown = CodeRules.check("feature/orders/src/main/kotlin/A.kt", "// allow(raw-px): legacy\nval p = 0.dp")
        val unused = CodeRules.check("feature/orders/src/main/kotlin/A.kt", "// allow(raw-dp): legacy\nval p = 0.dp")

        assertEquals(listOf(1 to "allow"), unknown.map { it.line to it.rule })
        assertTrue(unknown.single().message.contains("raw-px"))
        assertEquals(listOf(1 to "allow"), unused.map { it.line to it.rule })
        assertTrue(unused.single().message.contains("delete the marker"))
    }

    @Test
    fun `one marker can name two rules on the same line`() {
        val source = "// allow(raw-dp, raw-color): the brand mark is drawn to the logo's own spec\nval m = Mark(16.dp, Color(0xFF112233))"

        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/main/kotlin/A.kt", source))
    }

    @Test
    fun `patterns inside comments and strings are not code`() {
        val source = """
            // Dispatchers.IO and Modifier.clickable { } in a comment
            /* tween(300) /* nested */ runBlocking { } */
            val a = "Modifier.clickable { } and Color(0xFF000000)"
            val b = ${"\"\"\""}
                LazyColumn { items(rows) { } } and 16.dp
            ${"\"\"\""}
            val c = '"'
            fun `a test named runBlocking`() = Unit
        """.trimIndent()

        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/main/kotlin/A.kt", source))
    }

    @Test
    fun `code inside a string template is code`() {
        val source = "val a = \"on ${'$'}{Dispatchers.IO.toString()}\""

        assertEquals(listOf(1 to "dispatchers-io"), CodeRules.check("core/network/src/main/kotlin/A.kt", source).map { it.line to it.rule })
    }

    @Test
    fun `rules hold only in their scope`() {
        assertEquals(emptyList<Finding>(), CodeRules.check("core/ui/src/main/kotlin/A.kt", "val p = 16.dp"))
        assertEquals(emptyList<Finding>(), CodeRules.check("core/designsystem/src/main/kotlin/A.kt", "val c = Color(0xFF000000)"))
        assertEquals(emptyList<Finding>(), CodeRules.check("app/src/main/kotlin/com/acme/di/CoroutinesModule.kt", "val io = Dispatchers.IO"))
        assertEquals(emptyList<Finding>(), CodeRules.check("data/orders/src/test/kotlin/A.kt", "val v = runBlocking { 1 }"))
        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/test/kotlin/A.kt", "val c = Color(0xFF000000)"))
    }

    @Test
    fun `a modular impl module is a feature module`() {
        val findings = CodeRules.check("feature/orders/impl/src/main/kotlin/A.kt", "val p = 16.dp")

        assertEquals(listOf("raw-dp"), findings.map { it.rule })
    }

    @Test
    fun `zero and one dp are written raw, anything else is a token`() {
        val path = "feature/orders/src/main/kotlin/A.kt"

        assertEquals(emptyList<Finding>(), CodeRules.check(path, "val a = 0.dp\nval b = 1.dp\nval c = size.dp"))
        assertEquals(listOf(1), CodeRules.check(path, "val a = 0.5.dp").map { it.line })
    }

    @Test
    fun `a lazy row with a key passes, and items outside a lazy scope are not rows`() {
        val source = """
            fun List() = LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                items(rows, key = { it.id }) { Row(it) }
                items(count = 3, key = { index -> index }) { Skeleton() }
                item { Header(onClick = { load(repository.items()) }) }
            }
            fun load() {
                val all = items(store)
            }
        """.trimIndent()

        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/main/kotlin/A.kt", source))
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

        assertEquals(listOf(3 to "lazy-key"), CodeRules.check("core/ui/src/main/kotlin/A.kt", source).map { it.line to it.rule })
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

        assertEquals(emptyList<Finding>(), CodeRules.check("feature/orders/src/main/kotlin/A.kt", source))
    }

    private fun allowed(code: String, rule: String): String = code.lines().flatMap { line ->
        if ("/*!*/" in line) {
            listOf(line.takeWhile(Char::isWhitespace) + "// allow($rule): the fixture needs it", line)
        } else {
            listOf(line)
        }
    }.joinToString("\n")
}
