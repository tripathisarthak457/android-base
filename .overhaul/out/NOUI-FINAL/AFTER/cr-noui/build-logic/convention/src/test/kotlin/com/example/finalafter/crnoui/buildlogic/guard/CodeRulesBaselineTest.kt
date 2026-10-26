package com.example.finalafter.crnoui.buildlogic.guard

import com.example.finalafter.crnoui.buildlogic.guard.CodeRules.Finding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeRulesBaselineTest {

    private val baseline = CodeRulesBaseline.parse(
        """
        # accepted until the design system's sweep
        core/ui/src/main/kotlin/A.kt raw-tween 2
        core/ui/src/main/kotlin/Gone.kt raw-tween 1
        """.trimIndent(),
    )

    @Test
    fun `findings up to the accepted count pass`() {
        val findings = mapOf(FILE to listOf(finding(3, "raw-tween"), finding(9, "raw-tween")))

        assertEquals(emptyList<String>(), baseline.judge(findings))
    }

    @Test
    fun `a finding beyond the count fails with every line of that rule, and other rules are not accepted`() {
        val findings = mapOf(
            FILE to listOf(finding(3, "raw-tween"), finding(9, "raw-tween"), finding(12, "raw-tween"), finding(4, "raw-color")),
        )

        assertEquals(
            listOf("$FILE:3 raw-tween: m", "$FILE:9 raw-tween: m", "$FILE:12 raw-tween: m", "$FILE:4 raw-color: m"),
            baseline.judge(findings),
        )
    }

    @Test
    fun `an entry above what the file breaks fails, so the list only shrinks`() {
        val reports = baseline.judge(mapOf(FILE to listOf(finding(3, "raw-tween"))))

        assertEquals(1, reports.size)
        assertTrue(reports.single().contains("lower the count"))
    }

    @Test
    fun `an entry for a file this module did not scan is someone else's`() {
        assertEquals(emptyList<String>(), baseline.judge(mapOf("core/ui/src/main/kotlin/B.kt" to emptyList())))
    }

    @Test
    fun `one list serves the compact project and its modular split`() {
        val split = CodeRulesBaseline.parse("feature/orders/src/main/kotlin/OrdersScreen.kt adaptive-preview 1")

        val findings = mapOf("feature/orders/impl/src/main/kotlin/OrdersScreen.kt" to listOf(finding(5, "adaptive-preview")))

        assertEquals(emptyList<String>(), split.judge(findings))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a malformed line is refused`() {
        CodeRulesBaseline.parse("core/ui/src/main/kotlin/A.kt raw-tween")
    }

    private fun finding(line: Int, rule: String) = Finding(line, rule, "m")

    private companion object {
        const val FILE = "core/ui/src/main/kotlin/A.kt"
    }
}
