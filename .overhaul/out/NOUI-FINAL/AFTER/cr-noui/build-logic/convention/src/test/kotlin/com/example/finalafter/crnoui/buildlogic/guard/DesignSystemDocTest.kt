package com.example.finalafter.crnoui.buildlogic.guard

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * docs/DESIGN-SYSTEM.md is where a component is learned: a template to copy, its test, and a
 * component the build refuses with each line marked by the rule that reports it. The rules run
 * over all three here, so the page cannot teach what the build fails, or name a rule that does not
 * fire where it says.
 */
class DesignSystemDocTest {

    private val doc = File(DOC).readText().replace("\r\n", "\n")

    private fun block(tag: String): String {
        val match = Regex("""(?ms)^```kotlin $tag\n(.*?)^```$""").find(doc)
        return checkNotNull(match) { "$DOC has no ```kotlin $tag block" }.groupValues[1]
    }

    private fun findings(path: String, source: String): List<String> =
        CodeRules.check(path, source).map { "${it.line} ${it.rule}" }

    @Test
    fun `the component template breaks no rule`() {
        assertEquals(emptyList<String>(), findings(COMPONENT, block("component-template")))
    }

    @Test
    fun `the component test template breaks no rule`() {
        val template = block("component-test-template")

        assertEquals(emptyList<String>(), findings(COMPONENT_TEST, template))
    }

    @Test
    fun `each line the refused component marks breaks exactly the rules it names`() {
        val lines = block("component-dont").lines()
        val marked = lines.flatMapIndexed { index, line ->
            val rules = MARK.find(line)?.groupValues?.get(1) ?: return@flatMapIndexed emptyList()
            rules.split(',').map { "${index + 1} ${it.trim()}" }
        }
        assertTrue("the refused component marks no line", marked.isNotEmpty())

        val unmarked = lines.joinToString("\n") { it.replace(MARK, "") }

        assertEquals(marked.sorted(), findings(COMPONENT, unmarked).sorted())
    }

    private companion object {
        /** Gradle runs the build logic's tests in build-logic/convention. */
        const val DOC = "../../docs/DESIGN-SYSTEM.md"
        const val DESIGN =
            "core/designsystem/src/%s/kotlin/com/acme/core/designsystem/component/tag"
        val COMPONENT = DESIGN.format("main") + "/AppTag.kt"
        val COMPONENT_TEST = DESIGN.format("test") + "/AppTagTest.kt"

        /** A trailing `// rule-id, rule-id` on a line of the refused component. */
        val MARK = Regex("""\s*//\s*([a-z][a-z0-9-]*(?:,\s*[a-z][a-z0-9-]*)*)\s*$""")
    }
}
