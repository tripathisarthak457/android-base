package com.example.standard.buildlogic.guard

import org.junit.Assert.assertEquals
import org.junit.Test

/** The cases of the sheet rules a single fixture cannot show: what each must leave alone. */
class SheetRuleCasesTest {

    private fun findings(path: String, code: String, rule: String): List<Int> =
        CodeRules.check(path, code.trimIndent()).filter { it.rule == rule }.map { it.line }

    @Test
    fun `IME padding outside a sheet, in a screen of its own, is the screen's business`() {
        val code = """
            @Composable
            private fun Orders() {
                Column(Modifier.imePadding()) { }
            }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code, "sheet-ime-padding"))
    }

    @Test
    fun `IME padding in a provided footer is flagged`() {
        val code = """
            @Composable
            private fun Footer() {
                ProvideSheetFooter {
                    Row(Modifier.windowInsetsPadding(WindowInsets.ime)) { }
                }
            }
        """
        assertEquals(listOf(4), findings(FEATURE, code, "sheet-ime-padding"))
    }

    @Test
    fun `a sheet driven by visible, even inside a column, is fine`() {
        val code = """
            @Composable
            private fun Orders(show: Boolean) {
                Column {
                    AppSheet(visible = show, onDismissRequest = {}) { }
                }
            }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code, "sheet-conditional"))
    }

    @Test
    fun `a sheet in a when branch or a lazy item is flagged`() {
        val code = """
            @Composable
            private fun Orders(mode: Int) {
                when (mode) {
                    0 -> AppSheet(visible = true, onDismissRequest = {}) { }
                    else -> Unit
                }
                LazyColumn {
                    item {
                        AppSheet(visible = true, onDismissRequest = {}) { }
                    }
                }
            }
        """
        assertEquals(listOf(4, 9), findings(FEATURE, code, "sheet-conditional"))
    }

    @Test
    fun `a test tag on the sheet is what its modifier is for`() {
        val code = """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}, modifier = Modifier.testTag("filters")) { }
            }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code, "sheet-modifier"))
    }

    @Test
    fun `effects, guards and overlays beside the root are not a second root`() {
        val code = """
            @Composable
            private fun Orders() {
                AppSheet(visible = true, onDismissRequest = {}) {
                    SheetDismissConfirmation(enabled = true, onDiscard = {})
                    LaunchedEffect(Unit) { }
                    AppAlert(visible = false, title = "", confirmLabel = "", onConfirm = {}, onDismissRequest = {})
                    LazyColumn(contentPadding = contentPadding) { }
                }
            }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, code, "sheet-root"))
    }

    @Test
    fun `a when over a sheet type with an else branch is fine, and the design system is left alone`() {
        val withElse = """
            private fun label(d: SheetDetent) = when (d) {
                SheetDetent.Large -> "large"
                else -> "other"
            }
        """
        assertEquals(emptyList<Int>(), findings(FEATURE, withElse, "sheet-when-else"))
        val exhaustive = """
            private fun label(d: SheetDetent) = when (d) {
                SheetDetent.Large -> "large"
                SheetDetent.Medium -> "medium"
            }
        """
        assertEquals(emptyList<Int>(), findings(DESIGN_SYSTEM, exhaustive, "sheet-when-else"))
    }

    @Test
    fun `the shadow and window rules hold only in the sheet's own package`() {
        assertEquals(emptyList<Int>(), findings(DESIGN_SYSTEM, "private fun Modifier.card() = this.shadow(8.dp)", "sheet-shadow"))
        assertEquals(emptyList<Int>(), findings(DESIGN_SYSTEM, "@Composable fun Menu() { Popup(onDismissRequest = {}) { } }", "sheet-window"))
    }

    private companion object {
        const val FEATURE = "feature/orders/src/main/kotlin/A.kt"
        const val DESIGN_SYSTEM = "core/designsystem/src/main/kotlin/A.kt"
    }
}
